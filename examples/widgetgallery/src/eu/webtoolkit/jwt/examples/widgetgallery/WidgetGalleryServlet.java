package eu.webtoolkit.jwt.examples.widgetgallery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import eu.webtoolkit.jwt.Configuration;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WBootstrap2Theme;
import eu.webtoolkit.jwt.WBootstrap3Theme;
import eu.webtoolkit.jwt.WBootstrap5Theme;
import eu.webtoolkit.jwt.WCssTheme;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WLinkedCssStyleSheet;
import eu.webtoolkit.jwt.WXmlLocalizedStrings;
import eu.webtoolkit.jwt.WtServlet;

public class WidgetGalleryServlet extends WtServlet {
	public WidgetGalleryServlet() {
		setConfiguration(new Configuration() {
			{
				setErrorReporting(ErrorReporting.NoErrors);
				setUaCompatible("IE8=IE7");
				setTinyMCEVersion(4);
				getProperties().put("leafletJSURL", "https://unpkg.com/leaflet@1.5.1/dist/leaflet.js");
				getProperties().put("leafletCSSURL", "https://unpkg.com/leaflet@1.5.1/dist/leaflet.css");
        setBotList(new ArrayList<String>(
          List.of(".*Slurp.*",
                  ".*crawl.*",
                  ".*Crawl.*",
                  ".*bot.*",
                  ".*Bot.*",
                  ".*spider.*",
                  ".*Spider.*",
                  ".*ia_archiver.*",
                  ".*Twiceler.*",
                  ".*Yandex.*",
                  ".*Nutch.*",
                  ".*Ezooms.*",
                  ".*Scrapy.*",
                  ".*Buck.*",
                  ".*Barkrowler.*",
                  ".*Censys.*",
                  ".*Blogtrottr.*",
                  ".*InternetMeasurement.*",
                  ".*Owler.*",
                  ".*centuryb.o.t9.*",
                  ".*Turnitin.*",
                  ".*MatchorySearch.*",
                  ".*newspaper.*",
                  ".*Go-http-client.*",
                  ".*Mojolicious.*",
                  ".*python-requests.*",
                  ".*Python.*",
                  ".*python-urllib.*",
                  ".*Apache-HttpClient.*",
                  ".*com.apple.WebKit.Networking.*",
                  ".*NetworkingExtension.*")));
			}

			@Override
			public boolean progressiveBootstrap(String internalPath) {
				if (internalPath.equals("/trees-tables/mvc-table-views") ||
					internalPath.equals("/trees-tables/mvc-tree-views") ||
					internalPath.equals("/trees-tables/mvc-item-models") ||
					internalPath.equals("/layout/layout-managers") ||
					internalPath.equals("/forms/line_text-editor") ||
					internalPath.startsWith("/graphics-charts"))
					return false;
				else
					return true;
			}
		});
		
	}
	
	@Override
	public WApplication createApplication(WEnvironment env) {
		WApplication app = new WApplication(env);

		String themeStr = app.getEnvironment().getParameter("theme");
		if (themeStr == null) {
			themeStr = "jwt";
		}

		if (Objects.equals(themeStr, "wt") ||
			Objects.equals(themeStr, "jwt")) {
			final String theme = themeStr;
			app.setTheme(new WBootstrap5Theme() {
				@Override
				public List<WLinkedCssStyleSheet> getStyleSheets() {
					return List.of(new WLinkedCssStyleSheet(new WLink("style/" + theme + ".css")));
				}
			});
		} else if (Objects.equals(themeStr, "bootstrap5")) {
			app.setTheme(new WBootstrap5Theme());
		} else if (Objects.equals(themeStr, "bootstrap3")) {
			WBootstrap3Theme theme = new WBootstrap3Theme();
			theme.setResponsive(true);
			app.setTheme(theme);

			// Load the default bootstrap3 (sub-)theme
			app.useStyleSheet(
					new WLinkedCssStyleSheet(
							new WLink(
					WApplication.getResourcesUrl() + "themes/bootstrap/3/bootstrap-theme.min.css")));
		} else if (Objects.equals(themeStr, "bootstrap2")) {
			WBootstrap2Theme theme = new WBootstrap2Theme();
			theme.setResponsive(true);
			app.setTheme(theme);
		} else {
			app.setTheme(new WCssTheme(themeStr));
		}

		WXmlLocalizedStrings resourceBundle = new WXmlLocalizedStrings();
		resourceBundle.use("/eu/webtoolkit/jwt/examples/widgetgallery/text");
		resourceBundle.use("/eu/webtoolkit/jwt/examples/widgetgallery/report");
		resourceBundle.use("/eu/webtoolkit/jwt/examples/widgetgallery/tpl");
		resourceBundle.use("/eu/webtoolkit/jwt/examples/widgetgallery/src");
		app.setLocalizedStrings(resourceBundle);

		app.getRoot().addWidget(new WidgetGallery());
		
		app.setTitle("JWt Widget Gallery");
		
		app.useStyleSheet(new WLink("style/widgetgallery.css"));
		app.useStyleSheet(new WLink("style/everywidget.css"));
		app.useStyleSheet(new WLink("style/pygments.css"));
		app.useStyleSheet(new WLink("style/layout.css"));
		app.useStyleSheet(new WLink("style/filedrop.css"));

		return app;
	}
}
