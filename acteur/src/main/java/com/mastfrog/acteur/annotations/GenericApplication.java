package com.mastfrog.acteur.annotations;

import com.google.inject.ImplementedBy;
import com.google.inject.name.Named;
import com.mastfrog.acteur.Application;
import com.mastfrog.acteur.ImplicitBindings;
import com.mastfrog.acteur.Page;
import static com.mastfrog.acteur.annotations.GenericApplicationModule.EXCLUDED_CLASSES;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.Path;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mastfrog.util.Strings;
import com.mastfrog.util.strings.AlignedText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

/**
 * An application which looks up its pages using registry files on the
 * classpath, generated by an annotation processor which processes
 * &#064;HttpCall annotations.
 *
 * @author Tim Boudreau
 */
@SuppressWarnings("deprecation")
public class GenericApplication extends Application {

    public GenericApplication(boolean withHelp) {
        // Even though the varags version is semantically identical, Guice will
        // attempt to inject a Class[] into it and fail
        this(new GenericApplicationSettingsImpl(), new Class<?>[0]);
    }

    public GenericApplication() {
        // Even though the varags version is semantically identical, Guice will
        // attempt to inject a Class[] into it and fail
        this(new Class<?>[0]);
    }

    public GenericApplication(@Named(EXCLUDED_CLASSES) Class<?>... excludePages) {
        this(new GenericApplicationSettingsImpl(), excludePages);
    }

    @Inject
    @SuppressWarnings("deprecation")
    public GenericApplication(GenericApplicationSettings settings, @Named(EXCLUDED_CLASSES) Class<?>... excludePages) {
        boolean helpEnabled = settings.helpEnabled;

        Set<Class<?>> excluded = new HashSet<>(Arrays.asList(excludePages));
        ImplicitBindings implicit = getClass().getAnnotation(ImplicitBindings.class);
        Set<Class<?>> alreadyBound = implicit == null ? Collections.<Class<?>>emptySet()
                : new HashSet<>(Arrays.asList(implicit.value()));
        HttpCallRegistryLoader loader = new HttpCallRegistryLoader(getClass());
        List<Class<? extends Page>> originalOrder = new ArrayList<>();
        // Log first, then add
        // Preserve original sort order in case of backward compatibility issues
        // load order shouldn't matter, but it could change where things fail in
        // case of misconfiguration
        for (Class<? extends Page> c : loader) {
            originalOrder.add(c);
        }
        if (Boolean.getBoolean("acteur.debug")) {
            logClasses(originalOrder, alreadyBound, excluded, helpEnabled);
        }
        for (Class<? extends Page> pageType : originalOrder) {
            if (!alreadyBound.contains(pageType) && !excluded.contains(pageType)) {
                add(pageType);
            }
        }
        if (helpEnabled) {
            add(Application.helpPageType());
        }
        if (settings.corsEnabled) {
            super.enableDefaultCorsHandling();
        }
    }

    private static void logClasses(List<Class<? extends Page>> originalOrder, Set<Class<?>> alreadyBound, Set<Class<?>> excluded, boolean helpEnabled) {
        StringBuilder appInfo = new StringBuilder();

        List<Class<? extends Page>> sorted = new ArrayList<>(originalOrder);
        if (helpEnabled) {
            sorted.add(Application.helpPageType());
        }
        Collections.sort(sorted, (a, b) -> {
            int oa = orderOf(a);
            int ob = orderOf(b);
            return oa == ob ? 0 : oa > ob ? 1 : -1;
        });
        for (Class<? extends Page> pageType : sorted) {
            if (!alreadyBound.contains(pageType) && !excluded.contains(pageType)) {
                pageTypeToString(pageType, appInfo);
            }
        }
        System.err.println("Acteur application with the following HTTP calls:\n");
        System.err.println(AlignedText.formatTabbed(appInfo));
    }

    private static int orderOf(Class<? extends Page> pg) {
        HttpCall call = pg.getAnnotation(HttpCall.class);
        if (call != null) {
            return call.order();
        }
        return 0;
    }

    private static StringBuilder pageTypeToString(Class<? extends Page> pg, StringBuilder into) {
        into.append(" * ");
        String nm = pg.getSimpleName();
        if (nm.endsWith("__GenPage")) {
            nm = nm.substring(0, nm.length() - "__GenPage".length());
        }
        String pkg = pg.getPackage().toString();
        into.append(nm).append('\t');
        method(pg, into).append('\t').append('\t');
        path(pg, into).append('\t').append(" (").append(pkg).append(')').append('\n');
        return into;
    }

    private static StringBuilder method(Class<? extends Page> pg, StringBuilder into) {
        Methods mths = pg.getAnnotation(Methods.class);
        if (mths != null) {
            into.append(Strings.join('/', Arrays.asList(mths.value())));
        } else {
            into.append("???");
        }
        return into;
    }

    private static StringBuilder path(Class<? extends Page> pg, StringBuilder into) {
        Path path = pg.getAnnotation(Path.class);
        if (path != null) {
            into.append(Strings.join(", ", Arrays.asList(path.value())));
        } else {
            PathRegex regexen = pg.getAnnotation(PathRegex.class);
            if (regexen != null) {
                into.append(Strings.join(" ", Arrays.asList(regexen.value())));
            } else {
                into.append("/???????");
            }
        }
        return into;
    }

    @ImplementedBy(GenericApplicationSettingsImpl.class)
    public static class GenericApplicationSettings {

        public final boolean corsEnabled;
        public final boolean helpEnabled;

        public GenericApplicationSettings(boolean corsEnabled, boolean helpEnabled) {
            this.corsEnabled = corsEnabled;
            this.helpEnabled = helpEnabled;
        }
    }

    static class GenericApplicationSettingsImpl extends GenericApplicationSettings {

        GenericApplicationSettingsImpl() {
            super(false, false);
        }
    }
}
