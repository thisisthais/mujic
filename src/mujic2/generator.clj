(ns mujic2.generator)

(defn parse-notes-sequence
  "Takes a vec of [note duration] tuples, and returns something that
  can be used by overtone's phrase, a vec of durations scaled by the resolutionv
  of the input song and a vec of notes converted to pitches."
  [resolution notes-sequence]
  (let [ticks (map last notes-sequence)
        midi-notes (map first notes-sequence)
        durations (map #(/ % resolution) ticks)
        pitches (map #(rem % 12) midi-notes)]
    [durations pitches]))
