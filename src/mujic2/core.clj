(ns mujic2.core
  (:gen-class)
  (:require [mujic2.midi-parser :as parser]
            [mujic2.markov-chain :as chain]
            [clojure.pprint :refer [pprint]]))

(defn -main
  "Parses a Satie piano MIDI file and generates a nested map for the use in a
  markov chain. The map has nested keys, note and note duration. The value is
  a set of all the note:duration tuples that immediatelly follow the prefix
  note:duration in the song."
  [& args]

  (->> (parser/parse-midi-file "/Users/thaisc/mujic/satie.mid")
       (sort-by :tick)
       chain/notes->successive-notes
       (chain/generate-notes-sequence ["C5" 260] 10)
       pprint))
