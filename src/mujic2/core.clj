(ns mujic2.core
  (:gen-class)
  (:require [mujic2.midi-parser :as p])
  (:use [clojure.pprint]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [parsed (p/parse-midi-file "/Users/thaisc/mujic/satie.mid")]
   (pprint (p/notes->successive-notes (sort-by :tick parsed)))))
