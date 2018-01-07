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
  [parsed-event]
  (let [command (get parsed-event :command)]
    (or (= command :note-on) (= command :note-off))))

(defn filter-notes
  [parsed-events]
  (filter is-note parsed-events))

(defn get-tick
  [parsed-event]
  (get parsed-event :tick))

(defn get-note
  [parsed-event]
  (get parsed-event :note))

(defn create-chord-mapping
  [[tick event]]
  [key (set (mapv get-note event))])

(defn group-by-tick
  [parsed-events]
  (->> (filter-notes parsed-events)
       (group-by get-tick)
       (seq)
       (map (fn [[key value]]
              [key (set (mapv get-note value))]))
       (into {})))
