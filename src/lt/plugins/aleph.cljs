(ns lt.plugins.aleph
  (:require [lt.object :as object]
            [lt.objs.tabs :as tab]
            [lt.objs.command :as cmd]
            [lt.objs.sidebar.command :as fl]
            [clojure.set :as setop]
            [lt.objs.editor.pool :as pool])
  (:require-macros [lt.macros :refer [behavior background defui]]))


;;;;===========================================================================
;;;; relation functions
;;;;
;;;; Identification of basic associations between behaviors, objects, tags.
;;;; TODO: separate graphing and restructuring for filter-lists.
;;;;___________________________________________________________________________

;;; behavior to object/tag

(defn k|coll [b]
  ;; will be used when relating behaviors with arguments.
  (if (coll? b)
    (first b)
    b))

(defn b->t
  "Given a sequence of behaviors, returns a map of the associated tags and all
   their behaviors."
  [bs]
  (let [behs (apply hash-set bs)]
    (->> @object/tags
         (filter (fn [t]
                   (some behs (val t))))
         (into {}))))

(defn b->o
  "Given a sequence of behaviors, returns a list of the objects to which any of
   them is associated."
  [bs]
  (let [behs (apply hash-set bs)
        listeners (fn [o]
                    (->> @o :listeners vals (apply concat)))]
    (filter #(some behs (listeners %))
            (vals @object/instances))))


;;; object to behavior/tag

(defn o->t*
  "Given an object instance, returns a list of its tags."
  [o]
  (:tags @o))

(defn o->t
  "Given a sequence of objects ids, returns a list of its tags as keys associated
   to their behaviors."
  [ids]
  (let [os (map object/by-id ids)
        t-keys (distinct (mapcat o->t* os))
        t-vals (map @object/tags t-keys)]
    (zipmap t-keys t-vals)))

(defn o->b*
  "Given an object instance, returns a list of its behaviors' names."
  [o]
  (let [listeners (:listeners @o)
        behs (vals listeners)]
    (apply concat behs)))

(defn o->b
  "Given a sequence of object ids, returns a list of the associated behaviors."
  [ids]
  (let [os (map object/by-id ids)]
    (->> os
         (mapcat o->b*)
         distinct
         (map @object/behaviors))))


;;; tag to object/behavior

(defn ->behavior [beh]
  ;; review: drop the metadata and use a normal keyval
  (if (coll? beh)
    (with-meta (@object/behaviors (first beh)) {:with-args (into [] (rest beh))})
    (@object/behaviors beh)))

(defn t->b
  "Given a sequence of tags, returns a list of the associated behaviors.
   Arguments passed by a tag will be stored as metadata `:with-args [args]`
   in the entry for the associated behavior."
  [ts]
  (map ->behavior (object/tags->behaviors ts)))

(defn t->o
  "Given a sequence of tags, returns a map of associated objects."
  [ts]
  (mapcat object/by-tag ts))

(defn t-enlist [t]
 (map #(hash-map :tag (str (key %))
                 :behaviors (val %)) @t))

(def t-list (scmd/filter-list {:items (t-enlist object/tags)
                               :key :tag
                               :placeholder "Tag"}))


;;;; object to behavior/tag ;;;;

(defn o->t
  "Given an object instance, returns its tags."
  [o]
  (:tags @o))

(defn o->b
  "Given an object instance, return the associated behaviors."
  [o]
  (mapcat t->b (o->t o)))

(defn o-enlist [o]
  (map #(hash-map :type (-> % val deref :lt.object/type str)
                  :tags (-> % val deref :tags)
                  :listeners (-> % val deref :listeners)) @o))

(def o-list (scmd/filter-list {:items (o-enlist object/instances)
                               :key :type
                               :placeholder "Object"}))


;;;; behavior to object/tag ;;;;

(defn beh-k|coll?
  ;; todo: make this more readable
  [b]
  (if (coll? b)
    (fn [t] (some #(= b %) (val t)))
    (fn [t] (some #(= b %) (map #(if (coll? %)
                                   (first %)
                                   %)
                                (val t))))))

(defn b->t [b]
  (->> @object/tags
       (filter (beh-k|coll? b))
       (into {})))

(defn b->o [b]
  (mapcat #(object/by-tag %) (keys (b->t b))))

;;; behavior filter-list

(defn b-enlist [b]
  (map #(hash-map :name (str (:name %))
                  :triggers (:triggers %)) (vals @b)))

(def b-list (scmd/filter-list {:items (b-enlist object/behaviors)
                               :key :name
                               :placeholder "Behaviors"}))
