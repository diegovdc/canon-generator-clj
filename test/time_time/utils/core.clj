(ns time-time.utils.core
  (:require [clojure.spec.gen.alpha :as gen]))

(defn rotate [a n]
  (let [l (count a)
        off (mod (+ (mod n l) l) l)]
    (concat (drop off a) (take off a))))

(defn get-time-interval
  "Calculate time interval between pairs of timestamps from a list of timestamps"
  [times]
  (->> times
       (partition 2 1)
       (map (fn [[a b]] (- b a)))))

(defn close-to [test-num by num]
  (and (>= num (- test-num by))
       (<= num (+ test-num by))))

(defn ->positive>0 [n]
  (cond (= 0 n) 1
        (> 0 n) (* -1 n)
        :else n))

(defn ->positive>x [x n]
  (cond (= x n) 1
        (> x n) (+ x (->positive>0 n))
        :else n))


(defn gen-ratio
  ([] (gen/fmap ->positive>0 (gen/ratio)))
  ([greater-than]
   (gen/fmap (partial ->positive>x greater-than) (gen/ratio))))

(defn gen-durs []
  (gen/not-empty (gen/vector (gen/fmap ->positive>0 (gen/int)))))

(defn gen-cp []
  (gen/fmap ->positive>0 (gen/int)))

(defn get-next-n-events [durs voice n]
  (loop [n* n
         index (:index voice)
         res []]
    (if (= -1 n*)
      res
      (recur (dec n*)
             (inc index)
             (conj res
                   (merge
                    voice
                    (let [original-dur (nth durs (mod index (count durs)))
                          dur (* (voice :ratio) original-dur)
                          edq (:echoic-distance-event-qty (last res))]
                      {:durs durs
                       :index index
                       :dur dur
                       :original-dur original-dur
                       :elapsed (+ (get (last res) :dur 0)
                                   (get (last res)
                                        :elapsed
                                        (:elapsed voice)))
                       :echoic-distance-event-qty (if edq
                                                    (dec edq)
                                                    (:echoic-distance-event-qty voice))
                       :echoic-distance (if (empty? res)
                                          (voice :echoic-distance)
                                          (- (-> res
                                                 last
                                                 :echoic-distance)
                                             (-> res last :dur)))})))))))


(comment
  ;;test
  (get-next-n-events [1 1 1 1]
   {:echoic-distance 4 :elapsed 0 :index 0 :ratio 1 :echoic-distance-event-qty 4}
   4))

(defn get-next-event [voice durs]
  (merge voice {:index (inc (:index voice))
                :dur (* (voice :ratio)
                        (nth durs (mod (voice :index) (count durs))))
                :elapsed (+ (get voice :dur 0)
                            (get voice
                                 :elapsed
                                 (:elapsed voice)))}))
