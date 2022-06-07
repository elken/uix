(ns uix.compiler.attributes
  (:require [clojure.string :as str]
            [goog.object :as gobj]))

(declare convert-prop-value)
(declare convert-prop-value-shallow)

(defn js-val? [x]
  (not (identical? "object" (goog/typeOf x))))

(def prop-name-cache
  #js {:class "className"
       :for "htmlFor"
       :charset "charSet"})

(def custom-prop-name-cache #js {})

(def ^:private cc-regexp (js/RegExp. "-(\\w)" "g"))

(defn- cc-fn [s]
  (str/upper-case (aget s 1)))

(defn ^string dash-to-camel [^string name-str]
  (if (or (str/starts-with? name-str "aria-")
          (str/starts-with? name-str "data-"))
    name-str
    (.replace name-str cc-regexp cc-fn)))

(defn keyword->string [x]
  (if (keyword? x)
    (-name ^not-native x)
    x))

(defn cached-prop-name [k]
  (if (keyword? k)
    (let [name-str (-name ^not-native k)]
      (if-some [k' (aget prop-name-cache name-str)]
        k'
        (let [v (dash-to-camel name-str)]
          (aset prop-name-cache name-str v)
          v)))
    k))

(defn cached-custom-prop-name [k]
  (if (keyword? k)
    (let [name-str (-name ^not-native k)]
      (if-some [k' (aget custom-prop-name-cache name-str)]
        k'
        (let [v (dash-to-camel name-str)]
          (aset custom-prop-name-cache name-str v)
          v)))
    k))

(defn kv-conv [o k v]
  (gobj/set o (cached-prop-name k) (convert-prop-value v))
  o)

(defn kv-conv-shallow [o k v]
  (gobj/set o (cached-custom-prop-name k) v)
  o)

(defn custom-kv-conv [o k v]
  (gobj/set o (cached-custom-prop-name k) (convert-prop-value v))
  o)

(defn convert-prop-value [x]
  (cond
    (js-val? x) x
    (keyword? x) (-name ^not-native x)
    (map? x) (reduce-kv kv-conv #js {} x)
    (coll? x) (clj->js x)
    (ifn? x) #(apply x %&)
    :else (clj->js x)))

(defn convert-custom-prop-value [x]
  (cond
    (js-val? x) x
    (keyword? x) (-name ^not-native x)
    (map? x) (reduce-kv custom-kv-conv #js {} x)
    (coll? x) (clj->js x)
    (ifn? x) #(apply x %&)
    :else (clj->js x)))

(defn convert-prop-value-shallow [x]
  (if (map? x)
    (reduce-kv kv-conv-shallow #js {} x)
    x))

(defn class-names-coll [classes]
  (let [^js/Array classes (reduce (fn [^js/Array a c]
                                    (when ^boolean c
                                      (->> (if (keyword? c) (-name ^not-native c) c)
                                           (.push a)))
                                    a)
                                  #js []
                                  classes)]
    (when (pos? (.-length classes))
      (.join classes " "))))

(defn ^string class-names
  ([a]
   (cond
     (or (array? a) (coll? a)) (class-names-coll a)
     (keyword? a) (-name ^not-native a)
     :else a))
  ([a b]
   (if ^boolean a
     (if ^boolean b
       (str (class-names a) " " (class-names b))
       (class-names a))
     (class-names b)))
  ([a b & rst]
   (reduce class-names (class-names a b) rst)))

(defn set-id-class
  "Takes attributes map and parsed tag, and returns attributes merged with class names and id"
  [props id-class]
  (let [props-class (get props :class)
        id (aget id-class 1)
        class (aget id-class 2)]
    (cond-> props
            ;; Only use ID from tag keyword if no :id in props already
      (and (some? id) (nil? (get props :id)))
      (assoc :id id)

              ;; Merge classes
      (or class props-class)
      (assoc :class (class-names class props-class)))))

(defn convert-props
  "Converts `props` Clojure map into JS object suitable for
  passing as `props` object into `React.createElement`

  - `props` — Clojure map of props
  - `id-class` — a triplet of parsed tag, id and class names
  - `shallow?` — indicates whether `props` map should be converted shallowly or not"
  [props id-class ^boolean shallow?]
  (cond
    ^boolean (aget id-class 3)
    (convert-custom-prop-value (set-id-class props id-class))

    shallow?
    (convert-prop-value-shallow props)

    :else (convert-prop-value (set-id-class props id-class))))

(defn interpret-attrs
  "Returns a tuple of attributes and a child element

  - [attrs] when `attrs` is actually a map of attributes
  - [nil attrs] when `attrs` is not a map, thus a child element"
  [maybe-attrs id-class shallow?]
  (if (map? maybe-attrs)
    #js [(convert-props maybe-attrs id-class shallow?)]
    #js [(convert-props {} id-class shallow?) maybe-attrs]))

(defn interpret-props
  "Returns a tuple of component props and a child element

  - [props] when `props` is actually a map of attributes
  - [nil props] when `props` is not a map, thus a child element"
  [props]
  (if (map? props)
    #js [props]
    #js [nil props]))
