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
  [note later-events]
  (->> later-events
       (some #(and (= (:command %) :note-off)
                   (= (:note %) note)
                   (:tick %)))))

(defn sub-and-round-up
  "Subtracts timestamps and rounds up to nearest multiple of 10"
  [off-tick on-tick]
  (int (* 10 (Math/ceil (/ (- off-tick on-tick) 10)))))

(defn get-note-duration
  "Given a destructured note-on event and the events at a tick after it, find the
  notes duration"
  [{:keys [note tick]} later-events]
  (let [duration (sub-and-round-up (find-off-tick note later-events) tick)]
    [note duration]))

(defn get-notes-and-durations
  "Given note events that happen at the same tick, filter the note-on events,
  map those notes to their duration, and put it in a set."
  [next-note-events later-events]
  (->> next-note-events
      (filter #(= (:command %) :note-on))
      (map #(get-note-duration % later-events)) ;; filter for later events
      (set)))

(defn assoc-note-to-successive-notes
  "Processes the current note by determining its duration, fetching the notes that
  succeed it, unioning those notes in a set, and updating the outermost map keyed by
  the original note and its duration."
  [outer-map on-tick note events]
  (let [off-tick (find-off-tick note events)
        later-events (filter #(> (:tick %) off-tick) events)
        next-note-events (filter #(= (:tick %) off-tick) events)
        duration (sub-and-round-up off-tick on-tick)
        next-notes-set (get-notes-and-durations next-note-events later-events)]
    (update-in outer-map [note duration] #(set/union % next-notes-set))))

(defn notes->successive-notes
  "Takes an list of midi events, ordered by ticks, and produces a map where the nested keys
  are every note:duration pair in the song, and the value is a set of note:duration tuples
  that succeed the note:duration keys in the song. Succession is calculated within a margin
  of 10 ticks."
  [ordered-events]
  (loop [outer-map {}
         [{:keys [tick command note]} & events] ordered-events]
    (cond
      (empty? events) outer-map
      (not= command :note-on) (recur outer-map events)
      :else (recur (assoc-note-to-successive-notes outer-map tick note events) events))))
