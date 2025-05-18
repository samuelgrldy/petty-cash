# How to Spin-up and Poke the New Astro Component

1. **Start the REPL the way you always do**  
   (`lein with-profile dev repl`, Calva jack-in, etc.)

2. **Inside the REPL**  
   a. `(dev)`   → jumps you to the dev helper namespace (unchanged)  
   b. `(go)`    → *starts* the full Component system that now includes  
      `:order-store` (our new EDN store)

   You should see something like:
   ```
   INFO : Preparing the system
   INFO : Starting OrderStore data/orders.edn
   => :started
   ```

3. **Inspect the store**:
   ```clojure
   (require '[clojure.pprint :refer [pprint]])
   (def st (user/store))          ; convenience fn we added
   (pprint @(:db st))             ; current DB contents (likely empty)
   ```

4. **Manual round-trip test (optional)**:
   ```clojure
   (require '[app.astro.api       :as api]
            '[app.astro.transform :as xf]
            '[app.astro.store     :as store])

   ;; fetch one page from the remote API
   (def raw   (api/fetch-page {}))

   ;; transform the wire format → clj map
   (def orders (map xf/transform-order (:content raw)))

   ;; merge into the in-memory atom
   (swap! (:db st) store/merge-orders orders)

   ;; verify it landed
   (pprint (keys (:orders @(:db st))))   ; should list order-ids
   ```

5. **Stop / reload as usual**:  
   `(reset)` → stops the system, refreshes code and restarts via `go`.

Remember:  
- The system is now started with `(go)` instead of the old `(start)` helper.
