(ns mujic2.markov-chain
  (:require [clojure.set :as set]))


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
  "Given a note-on event and the events at a tick after it, find the note's duration"
  [note-on-event later-events]
  (let [{:keys [note tick]} note-on-event
        duration (sub-and-round-up (find-off-tick note later-events) tick)]
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
