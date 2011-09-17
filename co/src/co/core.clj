(ns co.core
  (:use [co.extract :only (clj-from-jars)]
        [local-file :only (file*)]))

(def *root* (.getAbsolutePath (file* "../clojars-sync")))

(def *root-clj* (.getAbsolutePath (file* "../clojars-clj")))

(defn process []
  (clj-from-jars *root* *root-clj*))
