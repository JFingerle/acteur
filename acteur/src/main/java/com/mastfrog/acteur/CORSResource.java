package com.mastfrog.acteur;

import com.google.inject.Inject;
import com.mastfrog.acteur.headers.HeaderValueType;
import com.mastfrog.acteur.headers.Headers;
import com.mastfrog.acteur.headers.Method;
import com.mastfrog.acteur.util.CacheControl;
import com.mastfrog.acteur.util.CacheControlTypes;
import com.mastfrog.settings.Settings;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.joda.time.Duration;

/**
 *
 * @author Tim Boudreau
 */
final class CORSResource extends Page {

    public static final String SETTINGS_KEY_CORS_MAX_AGE_MINUTES = "cors.max.age.minutes";
    public static final String SETTINGS_KEY_CORS_ALLOW_ORIGIN = "cors.allow.origin";
    public static final boolean DEFAULT_CORS_ENABLED = true;
    public static final long DEFAULT_CORS_MAX_AGE_MINUTES = 5;
    public static final String DEFAULT_CORS_ALLOW_ORIGIN = "*";

    private static final HeaderValueType<?>[] hdrs = new HeaderValueType<?>[]{
        Headers.CONTENT_TYPE,
        Headers.stringHeader(HttpHeaders.Names.ACCEPT),
        Headers.stringHeader("X-Requested-With")
    };

    private static final Method[] methods = new Method[]{
        Method.GET,
        Method.POST,
        Method.PUT,
        Method.DELETE,
        Method.OPTIONS
    };

    @Inject
    CORSResource(ActeurFactory af) {
        add(af.matchMethods(Method.OPTIONS));
        add(CorsHeaders.class);

        getResponseHeaders().setContentLength(0);
    }

    private static final class CorsHeaders extends Acteur {
        @Inject
        CorsHeaders(Settings settings) {
            String allowOrigin = settings.getString(SETTINGS_KEY_CORS_ALLOW_ORIGIN, DEFAULT_CORS_ALLOW_ORIGIN);
            Duration dur = Duration.standardMinutes(
                    settings.getLong(SETTINGS_KEY_CORS_MAX_AGE_MINUTES, DEFAULT_CORS_MAX_AGE_MINUTES));
            add(Headers.ACCESS_CONTROL_ALLOW_ORIGIN, allowOrigin);
            add(Headers.ACCESS_CONTROL_ALLOW, methods);
            add(Headers.ACCESS_CONTROL_ALLOW_HEADERS, hdrs);
            add(Headers.ACCESS_CONTROL_MAX_AGE, dur);
            add(Headers.CACHE_CONTROL, CacheControl.$(CacheControlTypes.Public).add(CacheControlTypes.max_age, Duration.standardDays(365)));
            setState(new RespondWith(HttpResponseStatus.NO_CONTENT));
        }
    }
}
