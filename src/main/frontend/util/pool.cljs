(ns frontend.util.pool
  (:require ["threads" :refer [spawn Pool Worker]]
            [promesa.core :as p]
            [frontend.util :as util]
            ["path" :as path]
            [electron.ipc :as ipc]
            [frontend.config :as config]))

(defonce parser-pool (atom nil))

(defn create-parser-pool!
  ([]
   (create-parser-pool! 8))
  ([num]
   (p/let [static-path (if (util/electron?)
                         (ipc/ipc :getDirname)
                         "/static")]
     (Pool.
      (fn []
        (spawn (Worker. (str static-path "/js/parser-worker.js")) num))))))

;; (defn finish-pool!
;;   [{:keys [pool tasks]} ok-handler]
;;   (-> (p/all @tasks)
;;       (p/then (fn [result]
;;                 (ok-handler result)
;;                 (.completed pool)
;;                 (.terminate pool)
;;                 (reset! tasks nil)))))

(defn terminate-pool!
  [^js pool]
  (p/let [_ (.completed pool)]
    (.terminate pool)))

(defn terminate-parser-pool!
  []
  (when-let [pool @parser-pool]
    (terminate-pool! pool)))

(defn add-parse-job!
  [content config]
  (when-let [pool @parser-pool]
    (.queue ^js pool
            (fn [parser]
              (try
                (parser.parse content config)
                (catch js/Error e
                  (js/console.error e)
                  nil)))))
  ;; (let [task (.queue ^js pool
  ;;                    (fn [parser]
  ;;                      (parser.parse content config)))]
  ;;   (swap! (:tasks m) conj task)
  ;;   task)
  )

(defn init-parser-pool!
  []
  (p/let [pool (create-parser-pool!)]
    (reset! parser-pool pool)))

(comment
  (add-parse-job! "- hello" (frontend.format.mldoc/default-config :markdown))
  (add-parse-job! "*world*" (frontend.format.mldoc/default-config :markdown))

  )
