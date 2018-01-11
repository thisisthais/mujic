(ns mujic2.core
  (:gen-class)
  (:require [mujic2.midi-parser :as parser]
            [mujic2.markov-chain :as chain]
            [mujic2.generator :as generator]
            [clojure.pprint :refer [pprint]]))

(defn -main
  "Parses a Satie piano MIDI file and generates a nested map for the use in a
  markov chain. The map has nested keys, note and note duration. The value is
  a set of all the [note duration] tuples that immediatelly follow the prefix
  [note duration] in the song."
  [& args]

  (let [filepath "/Users/thaisc/mujic/sunflower-park.mid"
        resolution (parser/get-rounded-resolution filepath)
        parsed-midi (sort-by :tick (parser/parse-midi-file filepath))
        ;_ (pprint (take 10 parsed-midi))
        notes->successive-notes (chain/get-notes->successive-notes parsed-midi)
        ;_ (pprint notes->successive-notes)
        starting-note (chain/get-random-note notes->successive-notes)]
    (->> notes->successive-notes
         (chain/generate-notes-sequence starting-note 30)
         (generator/parse-notes-sequence resolution)
         print)))
