(require 'asdf)
(asdf:operate 'asdf:load-op 'cl-redis)

(defvar *bricksize* 100)
(defvar *lastbrick* nil)


(defun listtohash (l)
  (let ((h (make-hash-table)))
    (do ((li l (cddr li)))
        ((null li) 'done)
        (setf (gethash (car li) h) (cadr li))
        (print li))
    h))

(defun average (l)
  (/ (list-length l)
     (loop for i in l sum i)))


(redis:connect)
(red:ping)

(with-connection ()
  (red:subscribe "OrderBookEvent")
  (loop :for msg := (redis:expect :anything) :do
     (let* ((obe (listtohash (read-from-string msg)))
	    (avgprice (average (list (gethash :valuationBidPrice obe)
				     (gethash :valuationAskPrice obe))))))
     (if (< *bricksize* (abs (- avgprice *lastbrick*)))
	 (do (setf *lastbrick* (* (floor avgprice *bricksize*) *bricksize*))
	     (with-connection ()
	       (red:publish "RenkoBricks 1" *lastbrick*)))))
