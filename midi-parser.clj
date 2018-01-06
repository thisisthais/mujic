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

(def key-names '("C" "C#" "D" "D#" "E" "F" "F#" "G" "G#" "A" "A#" "B"))

(defn parse-midi-file [filepath]
  (let [sequence (-> (io/file filepath) MidiSystem/getSequence)
        tracks (.getTracks sequence)
        res (.getResolution sequence)
        events (get-all-events tracks)
        count (count tracks)]
    events))

(defn get-all-events
  "Takes an array of tracks and returns a coll of parsed events per track",
  [tracks]
  (map get-track-events tracks))

(defn get-track-events
  "Takes a track and returns a coll of its parsed events"
  [track]
  (let [size (.size track)
        index (take size (iterate inc 0))]
    (map #(parse-event (.get track %)) index)))

(defn parse-event
  "Takes an event, returns the event data parsed"
  [event]
  (let [message (.getMessage event)]
    (if (instance? ShortMessage message)
      (parse-message (cast ShortMessage message) event))))

(defn parse-message
  "Takes an event and its ShortMessage"
  [message, event]
  (let [tick (.getTick event)
        command (get command-map (.getCommand message))
        channel (.getChannel message)
        note (note-to-string (.getData1 message))
        velocity (.getData2 message)]
    {:tick tick :command command :channel channel :note note :velocity velocity}))

(defn note-to-string
  "Given a MIDI note number, returns the note name."
  [num]
  (let [note (rem num 12)
        octave (quot num 12)]
    (str (nth key-names note) octave)))
