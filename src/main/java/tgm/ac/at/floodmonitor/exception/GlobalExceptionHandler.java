package tgm.ac.at.floodmonitor.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Zentrale Fehlerbehandlung (Punkt 6) mit @RestControllerAdvice.
 *
 * Alle Fehler werden als ApiError zurueckgegeben und von Jackson automatisch
 * in JSON oder XML serialisiert - je nach Accept-Header des Clients.
 * Es werden keine Stacktraces ausgegeben.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ---------- 400 Bad Request ---------- */

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** Ungueltiges Datum oder ungueltiger Enum-Wert in einem Query-Parameter. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String msg = "Ungueltiger Wert fuer Parameter '" + ex.getName() + "'";
        return build(HttpStatus.BAD_REQUEST, msg, request);
    }

    /** Erforderlicher Query-Parameter fehlt. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String msg = "Erforderlicher Parameter '" + ex.getParameterName() + "' fehlt";
        return build(HttpStatus.BAD_REQUEST, msg, request);
    }

    /* ---------- 404 Not Found ---------- */

    @ExceptionHandler(StationNotFoundException.class)
    public ResponseEntity<ApiError> handleStationNotFound(StationNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /** Unbekannte URL -> ebenfalls 404 mit einheitlichem Fehler-Body. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Ressource wurde nicht gefunden", request);
    }

    /* ---------- 405 / 406 / 415 ---------- */

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "HTTP-Methode nicht erlaubt", request);
    }

    /** Nicht unterstuetztes Accept-Format (Punkt 5: 406 Not Acceptable). */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiError> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_ACCEPTABLE, "Format nicht unterstuetzt (Accept-Header pruefen)", request);
    }

    /** Nicht unterstuetzter Content-Type im Request-Body (Punkt 6: 415). */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Medientyp nicht unterstuetzt", request);
    }

    /* ---------- 500 Internal Server Error ---------- */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Interner Serverfehler", request);
    }

    /* ---------- Hilfsmethode ---------- */

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
