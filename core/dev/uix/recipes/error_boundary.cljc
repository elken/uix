(ns uix.recipes.error-boundary
  (:require [uix.core.alpha :as core]))

(def error-boundary
  (core/create-error-boundary
    {:error->state (fn [error] {:error error})
     :handle-catch (fn [error info] (println error))}
    (fn [{:keys [error]} [child]]
      (if error
        error
        child))))

(defn erroring-component []
  (throw "Hey!"))

(defn recipe []
  [error-boundary
   [erroring-component]])
