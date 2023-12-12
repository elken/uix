(ns uix.compiler.aot
  "Compiler code that translates HyperScript into React calls at compile-time."
  (:require [uix.compiler.js :as js]
            [uix.compiler.attributes :as attrs]
            [uix.lib]))

(defmulti compile-attrs
  "Compiles a map of attributes into JS object,
  or emits interpretation call for runtime, when a value
  at props position is dynamic (symbol)"
  (fn [tag attrs opts] tag))

(defmethod compile-attrs :element [_ attrs {:keys [tag-id-class]}]
  (if (or (map? attrs) (nil? attrs))
    `(cljs.core/array
      ~(cond-> attrs
         ;; merge parsed id and class with attrs map
         :always (attrs/set-id-class tag-id-class)
         ;; interpret :style if it's not map literal
         (and (some? (:style attrs))
              (not (map? (:style attrs))))
         (assoc :style `(uix.compiler.attributes/convert-props ~(:style attrs) (cljs.core/array) true))
         ;; camel-casify the map
         :always (attrs/compile-attrs {:custom-element? (last tag-id-class)})
         ;; emit JS object literal
         :always js/to-js))
    ;; otherwise emit interpretation call
    `(uix.compiler.attributes/interpret-attrs ~attrs (cljs.core/array ~@tag-id-class) false)))

(defmethod compile-attrs :component [_ props _]
  (if (or (map? props) (nil? props))
    `(cljs.core/array ~props)
    `(uix.compiler.attributes/interpret-props ~props)))

(defmethod compile-attrs :fragment [_ attrs _]
  (if (map? attrs)
    `(cljs.core/array ~(-> attrs attrs/compile-attrs js/to-js))
    `(uix.compiler.attributes/interpret-attrs ~attrs (cljs.core/array) false)))

(defn- input-component? [x]
  (contains? #{"input" "textarea"} x))

(defn form->element-type [tag]
  (cond
    (= :<> tag) :fragment
    (keyword? tag) :element

    (or (symbol? tag)
        (list? tag)
        (instance? clojure.lang.Cons tag))
    :component))

(defmulti compile-element*
  "Compiles UIx elements into React.createElement"
  (fn [[tag] _]
    (form->element-type tag)))

(defmethod compile-element* :default [[tag] _]
  (throw (AssertionError. (str "Incorrect element type. UIx elements can be one of the following types:\n"
                               "React Fragment: :<>\n"
                               "Primitive element: keyword\n"
                               "Component element: symbol"))))

(defmethod compile-element* :element [v {:keys [env]}]
  (let [[tag attrs & children] (uix.lib/normalize-element env v)
        tag-id-class (attrs/parse-tag tag)
        attrs-children (compile-attrs :element attrs {:tag-id-class tag-id-class})
        tag-str (first tag-id-class)
        ret (if (input-component? tag-str)
              `(create-uix-input ~tag-str ~attrs-children (cljs.core/array ~@children))
              `(>el ~tag-str ~attrs-children (cljs.core/array ~@children)))]
    ret))

(defmethod compile-element* :component [v {:keys [env]}]
  (let [[tag props & children] (uix.lib/normalize-element env v)
        tag (vary-meta tag assoc :tag 'js)
        props-children (compile-attrs :component props nil)]
    `(uix.compiler.alpha/component-element ~tag ~props-children (cljs.core/array ~@children))))

(defmethod compile-element* :fragment [v _]
  (let [[_ attrs & children] v
        attrs (compile-attrs :fragment attrs nil)
        ret `(>el fragment ~attrs (cljs.core/array ~@children))]
    ret))

(defn compile-element [v {:keys [env] :as opts}]
  (if (uix.lib/cljs-env? env)
    (compile-element* v opts)
    v))
