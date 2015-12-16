package gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import gov.usgs.net.HttpRequest;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.winston.Version;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;

/**
 *
 * @author Tom Parker
 *
 */
public abstract class FdsnUsageCommand extends FdsnCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(FdsnUsageCommand.class);

  protected String UrlBuillderTemplate;
  protected String InterfaceDescriptionTemplate;

  private Configuration cfg;

  public FdsnUsageCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
    initializeTemplateEngine();
  }

  /**
   * Initialize FreeMarker.
   * 
   * @throws IOException when things go wrong
   */
  private void initializeTemplateEngine() {
    cfg = new Configuration();
    cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "/www/fdsnws"));
    DefaultObjectWrapper obj = new DefaultObjectWrapper();
    obj.setExposeFields(true);
    cfg.setObjectWrapper(obj);

    cfg.setDefaultEncoding("UTF-8");
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
    cfg.setIncompatibleImprovements(new freemarker.template.Version(2, 3, 20));
  }

  @Override
  protected void sendResponse() {
    final HttpRequest req = new HttpRequest(cmd);
    Map<String, Object> root = new HashMap<String, Object>();
    root.put("baseUrl", "http://" + req.getHeader("Host") + "/");
    root.put("UrlBuilderTemplate", UrlBuillderTemplate);
    root.put("InterfaceDescriptionTemplate", InterfaceDescriptionTemplate);
    root.put("versionString", Version.VERSION_STRING);

    try {
      Template template = cfg.getTemplate("usage.ftl");
      Writer sw = new StringWriter();
      template.process(root, sw);
      String html = sw.toString();
      sw.close();

      final HttpResponse response = new HttpResponse("text/html; charset=utf-8");
      response.setLength(html.length());
      response.setCode("200");

      netTools.writeString(response.getHeaderString(), socketChannel);
      netTools.writeString(html, socketChannel);
    } catch (IOException e) {
      LOGGER.error(e.getLocalizedMessage());
    } catch (TemplateException e) {
      LOGGER.error(e.getLocalizedMessage());
    }
  }
}
