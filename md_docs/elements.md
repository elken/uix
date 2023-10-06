# Elements

UIx is using `$` to create elements that describe UI structure. There are 3 main types of elements: DOM elements, UIx component instances, and interop elements.

## DOM elements

`$` takes a tag name keyword, an optional map of attributes, and zero or more child elements.

```clojure
($ :button {:title "Submit"} "press me")
```

Element name is declared as a keyword with optional `id` and `class` attributes defined as a part of the name. Together they resemble CSS selector syntax.

```clojure
($ :div) ;; <div></div>
($ :h1.heading {:class "h1"} "👋") ;; <h1 class="heading h1">👋</h1>
($ :button#id.class1.class2) ;; <button id="id" class="class1 class2"></button>
```

## UIx component instances

Component instances are also created via the `$` macro call, where the first argument is the component function itself, the second argument is an optional map of props, and the rest are child elements.

```clojure
(defui button [{:keys [on-click children]}]
  ($ :button.btn {:on-click on-click}
    children))

($ button {:on-click #(js/console.log :click)}
  "press me")
```

## React component instances

React components written in JavaScript can be used directly in UIx with a minor differences in how props are passed into a component. See more detalis on the [“Interop with React”](/interop-with-react.html) page.

```clojure
($ Button {:on-click #(js/console.log :click)}
  "press me")
```
