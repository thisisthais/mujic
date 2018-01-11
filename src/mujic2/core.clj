(ns mujic2.core
  (:gen-class)
  (:require [mujic2.midi-parser :as parser]
            [mujic2.markov-chain :as chain]
            [mujic2.generator :as generator]
            [clojure.pprint :refer [pprint]]))

(defn -main
  "Parses a Satie piano MIDI file and generates a nested map for the use in a
  markov chain. The map has nested keys, note and note duration. The value is
  a set of all the note:duration tuples that immediatelly follow the prefix
  note:duration in the song."
  [& args]

  (let [filepath "/Users/thaisc/mujic/satie.mid"
        resolution (parser/get-rounded-resolution filepath)]
    (->> (parser/parse-midi-file filepath)
         (sort-by :tick)
         chain/notes->successive-notes
         (chain/generate-notes-sequence [60 260] 30)
         (generator/parse-notes-sequence resolution)
         print)))
