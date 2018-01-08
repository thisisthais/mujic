(require '[clojure.java.io :as io])
(import '(javax.sound.midi MidiSystem Sequence Track MidiEvent MidiMessage ShortMessage))

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

;; note that i think this only works for 1-track files not counting the metadata track
(defn parse-midi-file [filepath]
  "Given a midi file, outputs a human-readable collection of parsed event data"
  (let [sequence (MidiSystem/getSequence (io/file filepath))
        tracks (.getTracks sequence)
        resolution (.getResolution sequence) ;; gonna need later
        parsed-tracks (parse-tracks tracks)]
    (remove nil? (first (rest parsed-tracks)))))

(defn is-note
  "Returns true if this event is note on or note off event"
  [parsed-event]
  (let [command (get parsed-event :command)]
    (or (= command :note-on) (= command :note-off))))

(defn filter-notes
  "Returns only the note-relevant events"
  [parsed-events]
  (filter is-note parsed-events))

(defn get-notes-sets
  [acc parsed-event]
  (let [command (:command parsed-event)
        note (:note parsed-event)]
    (update acc command #(if % (conj % note) #{note}))))

(defn create-chord-mapping
  "Returns a map: key is the tick, value is a set of note events that happened at this tick"
  [acc tick events]
  (assoc acc tick (reduce get-notes-sets {} events)))

(defn group-by-tick
  [parsed-events]
  (->> (filter-notes parsed-events)
       (group-by :tick)
       (reduce-kv create-chord-mapping {})
       (into (sorted-map))))
(group-by-tick parsed)

(defn get-note-by-command
  "Returns a map: key is tick, value is set of notes that match given command at that tick"
  [grouped-notes-by-tick command-type]
  (reduce
    (fn [acc [tick notes-map]]
      (let [off-notes (command-type notes-map)]
        (update acc tick #(if % (conj % off-notes) off-notes))))
    {}
    grouped-notes-by-tick))
(get-note-offs grouped :note-off)

(defn get-first-note-tick
  "Given a note and ordered map of ticks to notes set, returns the first tick the note appears in"
  [note off-notes-map]
  (key (first (filter
                (fn [[tick notes-set]]
                  (contains? notes-set note))
                off-notes-map))))

;;(defn get-next-note-off
  ;;[note note-offs])

;; TODO pair on-off event times per note
;; per tick, per note-on event, find succeeding note-off's tick
(defn pair-on-off-ticks
  "Returns a map: key is the tick, value is {note -> tick for off event}"
  [grouped-note-events]
  ())
