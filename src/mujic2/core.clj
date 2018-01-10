(ns mujic2.core
  (:gen-class)
  (:require [mujic2.midi-parser :as parser
             mujic2.markov-chain :as chain])
  (:use [clojure.pprint]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [parsed (parser/parse-midi-file "/Users/thaisc/mujic/satie.mid")]
   (pprint (chain/notes->successive-notes (sort-by :tick parsed)))))
