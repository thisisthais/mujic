(ns mujic2.core
  (:gen-class)
  (:require [mujic2.midi-parser :as p])
  (:use [clojure.pprint]))

(def testn [{:tick 0, :command :note-on, :channel 0, :note "F3", :velocity 57}
            {:tick 256, :command :note-on, :channel 0, :note "C6", :velocity 52}
            {:tick 256, :command :note-on, :channel 0, :note "G#4", :velocity 57}
            {:tick 256, :command :note-on, :channel 0, :note "C5", :velocity 57}
            {:tick 256, :command :note-on, :channel 0, :note "F5", :velocity 57}
            {:tick 384, :command :note-on, :channel 0, :note "D#6", :velocity 61}
            {:tick 386, :command :note-off, :channel 0, :note "C6", :velocity 0}
            {:tick 386, :command :note-on, :channel 0, :note "B6", :velocity 0}
            {:tick 512, :command :note-on, :channel 0, :note "D6", :velocity 56}
            {:tick 514, :command :note-off, :channel 0, :note "D#6", :velocity 0}
            {:tick 768, :command :note-off, :channel 0, :note "G#4", :velocity 0}
            {:tick 768, :command :note-off, :channel 0, :note "C5", :velocity 0}
            {:tick 768, :command :note-off, :channel 0, :note "F5", :velocity 0}
            {:tick 773, :command :note-off, :channel 0, :note "D6", :velocity 0}
            {:tick 1024, :command :note-off, :channel 0, :note "F3", :velocity 0}
            {:tick 1024, :command :note-off, :channel 0, :note "B6", :velocity 0}])


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;(pprint (p/notes->successive-notes (sort-by :tick testn))))
  (let [parsed (p/parse-midi-file "/Users/thaisc/mujic/satie.mid")]
   (pprint (p/notes->successive-notes (sort-by :tick parsed)))))
