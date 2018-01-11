(ns mujic2.midi-parser
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]])
  (:import (javax.sound.midi MidiSystem Sequence Track MidiEvent MidiMessage ShortMessage)))


(def command->keyword
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
  "Given an event and its ShortMessage, creates a map of useful information
  parsed into a human-readable format."
  [event message]
  {:tick (.getTick event)
   :command (get command->keyword (.getCommand message))
   :channel (.getChannel message)
   ;:note (note-to-string (.getData1 message)) ;use for human reading notes
   :note (.getData1 message)
   :velocity (.getData2 message)})


(defn parse-event
  "Given an event, returns the parsed event data if the event does not represet metadata."
  [event]
  (let [message (.getMessage event)]
    (when (instance? ShortMessage message)
      (parse-message event message))))


(defn get-track-events
  "Given a track, returns a collection of its parsed events."
  [track]
  (let [size (.size track) ; java doesn't let me treat track as an array
        index (range size)]
    (map #(parse-event (.get track %)) index)))


(defn parse-tracks
  "Given an array of tracks, returns a collection of parsed events per track."
  [tracks]
  (map get-track-events tracks))


(defn note?
  "Returns true if this event is note on or note off event."
  [parsed-event]
  (let [command (get parsed-event :command)]
    (or (= command :note-on) (= command :note-off))))


(defn get-resolution
  "Given a path to a MIDI file, returns its resolution."
  [filepath]
  (-> (io/file filepath)
      MidiSystem/getSequence
      .getResolution))


(defn get-rounded-resolution
  "Gets the resolution for the given MIDI file but rounds it up to the nearest
  multiple of ten so that math with ticks works better (the same is done for ticks)"
  [filepath]
  (-> (get-resolution filepath)
      (/ 10)
      Math/ceil
      (* 10)
      int))


;; note that i think this only works for 1-track files not counting the metadata track
(defn parse-midi-file
  "Given a midi file, outputs a human-readable collection of parsed event data."
  [filepath]
  (let [sequence (MidiSystem/getSequence (io/file filepath))
        tracks (.getTracks sequence)
        resolution (.getResolution sequence) ;; gonna need later
        parsed-midi (parse-tracks tracks)
        [metadata & parsed-tracks] parsed-midi
        piano-track (first parsed-tracks)]
        ;piano-track (nth parsed-midi 2)] ;use for more complex song but figure out piano track
       (filter note? (remove nil? piano-track))))
