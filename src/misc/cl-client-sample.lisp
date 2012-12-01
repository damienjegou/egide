(require 'asdf)
(asdf:operate 'asdf:load-op 'cl-redis)

(redis:connect)
(red:ping)

(with-connection ()
  (red:subscribe "OrderBookEvent")
  (loop :for msg := (redis:expect :anything) :do
     (print msg)))
