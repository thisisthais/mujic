(ns mujic2.core
  (:gen-class)
  (:require [mujic2.midi-parser :as p]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [parsed (p/parse-midi-file "/Users/thaisc/old-mujic/satie.mid")]
   (p/notes->successive-notes (sort-by :tick parsed))))
