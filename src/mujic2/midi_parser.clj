(ns mujic2.midi-parser
  (:require [clojure.java.io :as io]
            [clojure.set :as set])
  (:import (javax.sound.midi MidiSystem Sequence Track MidiEvent MidiMessage ShortMessage)))

(def command-map
  {ShortMessage/CHANNEL_PRESSURE :channel-pressure
   ShortMessage/CONTROL_CHANGE   :control-change
   ShortMessage/NOTE_OFF         :note-off
   ShortMessage/NOTE_ON          :note-on
   ShortMessage/PITCH_BEND       :pitch-bend
   ShortMessage/POLY_PRESSURE    :poly-pressure
   ShortMessage/PROGRAM_CHANGE   :program-change})

(def key-names ["C" "C#" "D" "D#" "E" "F" "F#" "G" "G#" "A" "A#" "B"])

(defn note-to-string
  "Given a midi note number, returns the note name."
  [num]
  (let [note (rem num 12)
        octave (quot num 12)]
    (str (nth key-names note) octave)))

(defn parse-message
  "Given an event and its ShortMessage, parses useful information into human-readable format"
  [message, event]
  {:tick (.getTick event)
   :command (get command-map (.getCommand message))
   :channel (.getChannel message)
   :note (note-to-string (.getData1 message))
   :velocity (.getData2 message)})

(defn parse-event
  "Given an event, returns the parsed event data if the event does not represet metadata"
  [event]
  (let [message (.getMessage event)]
    (if (instance? ShortMessage message)
      (parse-message (cast ShortMessage message) event))))

(defn get-track-events
  "Given a track, returns a collection of its parsed events"
  [track]
  (let [size (.size track)
        index (range size)]
    (map #(parse-event (.get track %)) index)))

(defn parse-tracks
  "Given an array of tracks, returns a collection of parsed events per track"
  [tracks]
  (map get-track-events tracks))

(defn is-note
  "Returns true if this event is note on or note off event"
  [parsed-event]
  (let [command (get parsed-event :command)]
    (or (= command :note-on) (= command :note-off))))

(defn filter-notes
  "Returns only the note-relevant events"
  [parsed-events]
  (filter is-note parsed-events))

;; note that i think this only works for 1-track files not counting the metadata track
(defn parse-midi-file [filepath]
  "Given a midi file, outputs a human-readable collection of parsed event data"
  (let [sequence (MidiSystem/getSequence (io/file filepath))
        tracks (.getTracks sequence)
        resolution (.getResolution sequence) ;; gonna need later
        parsed-tracks (parse-tracks tracks)]
    (filter-notes (remove nil? (first (rest parsed-tracks))))))

(defn find-off-tick
  "Given a note and a sequence, returns the tick at which that note does note-off"
  [note later-events on-tick]
  (->> later-events
       (some #(and (= (:command %) :note-off)
                   (= (:note %) note)
                   (:tick %)))))

(defn assoc-end-tick
  "Associates a map of note to duration to a tick"
  [acc tick note events]
  ;; assoc-in creates a nested map, outer key is tick inner key is note
  (assoc-in acc [tick note] (find-off-tick note events)))

(defn pair-notes-to-end-tick
  "Loops through list of events. When it finds a note-on event, looks through the
  rest of the list to find its matching note-off event. Returns a map, keyed by tick
  whole values are a map of notes that went on at that tick and its duration."
  [ordered-events]
  (loop [acc {}
         ;; double deconstruct 1) first item & rest, 2) specific keys of first item
         [{:keys [tick command note]} & events] ordered-events]
    (cond
      ;; two base cases, empty list and not a note-on event
      (empty? events) acc
      (not= command :note-on) (recur acc events) ;; this automatically filters non-note events
      :else (recur (assoc-end-tick acc tick note events) events))))

(defn get-note-duration
  [{:keys [note tick]} later-events]
  (let [duration (- (find-off-tick note later-events tick) tick)]
    [note duration]))

(defn get-notes-and-durations
  [tick now-events later-events]
  (let [_ (prn "getting next notes for " now-events)]
    (->> now-events
      (filter #(= (:command %) :note-on))
      (map #(get-note-duration % later-events)) ;; filter for later events
      (set))))

(defn assoc-note-to-successive-notes
  [outer-map on-tick note events]
  (let [off-tick (find-off-tick note events on-tick)
        later-events (filter #(>= (:tick %) off-tick) events)
        now-events (filter #(= (:tick %) off-tick) events)
        duration (- off-tick on-tick)
        next-notes-set (get-notes-and-durations off-tick now-events later-events)]
    (update-in outer-map [note duration] #(set/union % next-notes-set))))

(defn notes->successive-notes
  [ordered-events]
  (loop [outer-map {}
         [{:keys [tick command note]} & events] ordered-events]
    (cond
      (empty? events) outer-map
      (not= command :note-on) (recur outer-map events)
      :else (recur (assoc-note-to-successive-notes outer-map tick note events) events))))
