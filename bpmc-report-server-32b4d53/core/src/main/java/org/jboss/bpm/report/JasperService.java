/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.bpm.report;

import org.apache.commons.logging.Log;

import org.apache.commons.logging.LogFactory;

//import org.eclipse.birt.core.framework.Platform;
//import org.eclipse.birt.report.engine.api.*;

import java.awt.Color;
import java.io.File;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRectangle;
import net.sf.jasperreports.engine.JRStyle;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.util.AbstractSampleApp;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRSaver;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.engine.design.JasperDesign;


import org.jboss.bpm.report.model.ReportParameter;
import org.jboss.bpm.report.model.ReportReference;
//import org.jboss.bpm.report.util.BirtUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * Core BIRT service component. Requires to step through the lifecycle
 * before it can be used:
 * <ul>
 * <li>create: constructs a BIRT engine and platform
 * <li>start:
 * <li>stop:
 * <li>destroy: destroy engine and shutdown platform
 *
 * @see org.eclipse.birt.report.engine.api.IReportEngine#destroy()
 * @see org.eclipse.birt.core.framework.Platform#shutdown()
 *
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class JasperService
{
  private static final Log log = LogFactory.getLog(JasperService.class);

  //private IReportEngine engine;
  private IntegrationConfig iConfig;

  private enum State  {NONE, CREATED, STARTED, STOPPED, DESTROYED};
  private State currentState = State.NONE;
  //private Map<String, IReportRunnable> cache = new ConcurrentHashMap<String, IReportRunnable>();
  private Map<String, ReportReference> reports = new ConcurrentHashMap<String, ReportReference>();
  private ServletContext servletContext;

  public JasperService(IntegrationConfig iConfig)
    {
      this.iConfig = iConfig;
    }

  /* blocking call*/
  public void create()
  {
    if(currentState!=State.NONE)
      throw new IllegalStateException("Service already in state " + currentState);

    synchronized(reports)
    {
      //this.engine = JasperEngineFactory.newInstance(iConfig);
      // parse template config
      try
      {
        loadReports();
        //compileReports();
        //extractParameterMetaData();
      }
      catch (Exception e)
      {
        throw new RuntimeException("Failed to load reports", e);
      }

      log.info("Jasper Service created: ");
      currentState = State.CREATED;
    }
  }

  private void loadReports()
  {
    log.info("in here......");
    File workDir = new File(iConfig.getReportDir());
    assert workDir.isDirectory();

    File[] reportFiles = workDir.listFiles(
        new FilenameFilter()
        {
          public boolean accept(File dir, String name)
          {
            return name.endsWith(".jasper");
          }
        }
    );

    for(File f : reportFiles)
    {
      reports.put(f.getName(), new ReportReference(f.getName()));
    }
    log.info("out reports size .."+reports.size());
  }

  private void compileReports(String metadataReportName)
  {
	// Run-time report parameters
		  Map parameters = new HashMap();
		  parameters.put("title", "A user-customized title");

	try {
		//log.info("Jasper Service created: ");
		//File sourceFile = new File(iConfig.getReportDir() + "/AlterDesignReport.jrxml");
		log.info(" metadataReportName : " + metadataReportName);

				File sourceFile = new File(iConfig.getReportDir() +"/overall_activity.jasper");
				//System.err.println(" : " + sourceFile.getAbsolutePath());
				JasperReport jasperReport = (JasperReport)JRLoader.loadObject(sourceFile);

				Connection conn = null;
				String url = "jdbc:h2:tcp://localhost/~/test";
				try
				{
					Class.forName("org.h2.Driver");
					conn = DriverManager.getConnection(url,"sa","");
				}
				catch(SQLException sqle){}
				catch(ClassNotFoundException cnfe){cnfe.printStackTrace();}


				JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, null, conn);

				File destFile = new File(sourceFile.getParent(), jasperReport.getName() + ".jrprint");
				JRSaver.saveObject(jasperPrint, destFile);
				JasperExportManager.exportReportToHtmlFile(destFile.getAbsolutePath(),iConfig.getOutputDir()+"/overall_activity.html");

	} catch (JRException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

  }
/*

  private void extractParameterMetaData() throws EngineException
  {
    Iterator<String> templateNames = reports.keySet().iterator();
    while(templateNames.hasNext())
    {
      String templateName = templateNames.next();
      //IReportRunnable template = openCached(templateName);

      // Update report reference details
      //String title = nonNull((String)template.getProperty(IReportRunnable.TITLE), "No title");
      //String description = nonNull((String)template.getProperty(IReportRunnable.DESCRIPTION), "No description");
      ReportReference reportRef = reports.get(templateName);
      reportRef.setTitle(title);
      reportRef.setDescription(description);

      Map<String, Map<String,Serializable>> paramDetails =
          new HashMap<String, Map<String, Serializable>>();

      //Create Parameter Definition Task and retrieve parameter definitions
      //IGetParameterDefinitionTask task = engine.createGetParameterDefinitionTask( template );
      Collection params = task.getParameterDefns( true );

      //Iterate over each parameter
      Iterator iter = params.iterator( );
      while ( iter.hasNext( ) )
      {
      //  IParameterDefnBase param = (IParameterDefnBase) iter.next( );

        if ( param instanceof IParameterGroupDefn )
        {
          IParameterGroupDefn group = (IParameterGroupDefn) param;

          // Do something with the parameter group.
          // Iterate over group contents.
          Iterator i2 = group.getContents( ).iterator( );
          while ( i2.hasNext( ) )
          {
            IScalarParameterDefn scalar = (IScalarParameterDefn) i2.next( );
            //Get details on the parameter
            paramDetails.put( scalar.getName(), BirtUtil.loadParameterDetails( task, scalar, template, group));
          }

        }
        else
        {
          IScalarParameterDefn scalar = (IScalarParameterDefn) param;
          //get details on the parameter
          paramDetails.put( scalar.getName(), BirtUtil.loadParameterDetails( task, scalar, template, null));
        }

        // cleanup
        task.close();
      }

      // update ReportReference.parameters
      ReportReference ref = reports.get(templateName);
      Iterator<String> paramNames = paramDetails.keySet().iterator();
      while(paramNames.hasNext())
      {
        Map<String, Serializable> map = paramDetails.get(paramNames.next());
        ReportParameter paramRef = new ReportParameter(
            (String)map.get("Name"), ReportParameter.Type.valueOf((String)map.get("Type"))
        );
        paramRef.setDataType(ReportParameter.DataType.valueOf((String)map.get("DataType")));
        paramRef.setHelptext((String)map.get("Help Text"));
        paramRef.setPromptText((String)map.get("Prompt Text"));
        ref.getParameterMetaData().add(paramRef);
      }


    }


  }
*/
  private static String nonNull(String candidate, String defaultValue)
  {
    return candidate != null ? candidate : defaultValue;
  }

  /* non blocking init */
  public void createAsync()
  {
    Thread t = new Thread(
        new Runnable()
        {
          public void run()
          {
            log.info("Creating Jasper service in the background");
            create();
          }
        }
    );

    t.setName("JasperService-Init");
    t.start();
  }

  public void start()
  {
    if(currentState!=State.CREATED)
      throw new IllegalStateException("Please call create lifecylce before starting the service");

    currentState = State.STARTED;
  }

  public void stop()
  {
    if(currentState!=State.STARTED)
      throw new IllegalStateException("Please call start lifecylce before stopping the service");

    currentState = State.STOPPED;
  }

  public void destroy()
  {
    //if(engine!=null)
      //engine.destroy();
    //Platform.shutdown();
    log.info("Service destroyed");
    currentState = State.DESTROYED;
  }

  public String render(RenderMetaData metaData)
  {

    if( (currentState==State.CREATED || currentState==State.STARTED) == false)
      throw new IllegalStateException("Cannot render in state " + currentState);

    if(!reports.keySet().contains(metaData.getReportName()))
      throw new IllegalArgumentException("No such report template: "+metaData.getReportName());

    String outputFileName = null;

    log.debug("Render " + metaData);

    //IRunTask task = null;
	/*
    try
    {
      //Open a (cached) report design
      //IReportRunnable design = openCached(metaData.getReportName());

      //Create runRenderTask to run and render the report,
      //task = engine.createRunTask(design);

      // outputfilename
      outputFileName = metaData.getReportName()+".html";

      //Set parent classloader for engine
      //task.getAppContext().put(EngineConstants.APPCONTEXT_CLASSLOADER_KEY, metaData.getClassloader());
      //task.setParameterValues(unmarshalParameters(metaData));
      //boolean validParams = task.validateParameters();
      //if(!validParams) {
    //	  log.error("Invalid report parameters " + task.getErrors());
      //}
      //task.run(servletContext.getRealPath("/WEB-INF" + iConfig.getOutputDir()) + "/" + outputFileName);
    }
    catch (EngineException e)
    {
      throw new RuntimeException("Failed to render report: "+e.getMessage(), e);
    }
    finally
    {
      if(task!=null)
        task.close();
    }
*/
			compileReports(metaData.getReportName());
			outputFileName = metaData.getReportName()+".html";



    return outputFileName;
  }

  private Map<String,Object> unmarshalParameters(RenderMetaData metaData)
  {
    Map<String, Object> results = new HashMap<String,Object>();
    for(String paramName : metaData.getParameters().keySet())
    {
      try
      {
        ReportReference reportRef = reports.get(metaData.getReportName());
        ReportParameter reportParam = reportRef.getParameter(paramName);
        if(null==paramName)
          throw new IllegalArgumentException("No such param "+paramName);

        String paramValue = metaData.getParameters().get(paramName);
        switch(reportParam.getDataType())
        {
          case DATETIME:
            results.put(paramName,
                new java.sql.Date(new SimpleDateFormat("yyyy-MM-dd").parse(paramValue).getTime()
                ));
            break;
          case NUMBER:
            results.put(paramName, Long.valueOf(paramValue));
            break;
          case BOOLEAN:
            results.put(paramName, Boolean.valueOf(paramValue));
            break;
          case STRING:
        	 results.put(paramName, String.valueOf(paramValue));
        	 break;
          default:
            results.put(paramName, paramValue);
        }
      }
      catch (Exception e)
      {
        throw new RuntimeException("Failed to unmarshall report parameter", e);
      }
    }
    return results;
  }

  /**
   * Render a report based on render meta data.
   * this routine simply return the output filename.
   * in order to get to an absolute path you need to prepend the service output directory:<p>
   * <code>
   *  BirtService.getIntegrationConfig().getOutputDir() + outputFileName;
   * </code>
   *
   * @param metaData render instruction like the template name
   * @return output filename
   */

  public String view(RenderMetaData metaData)
    {
      if( (currentState==State.CREATED || currentState==State.STARTED) == false)
        throw new IllegalStateException("Cannot renderTask in state " + currentState);

      if(!reports.keySet().contains(metaData.getReportName()))
        throw new IllegalArgumentException("No such report template: "+metaData.getReportName());

      String outputFileName = null;

      log.debug("View " + metaData);

      //IRenderTask renderTask = null;

      try
      {
        //Open a (cached) report design
        //IReportDocument document = engine.openReportDocument(
          //  iConfig.getOutputDir()+metaData.getReportName()+".rptdocument"
       // );

        //Create renderTask to run and renderTask the report,
       // renderTask = engine.createRenderTask(document);

        //Set parent classloader for engine
       // renderTask.getAppContext().put(EngineConstants.APPCONTEXT_CLASSLOADER_KEY, metaData.getClassloader());
       // renderTask.setParameterValues(metaData.getParameters());

        // ------------------

       // IRenderOption options = new RenderOption();
        switch(metaData.getFormat())
        {
          case HTML:
            //options.setOutputFormat("html");
            outputFileName = extactReportName(metaData.getReportName())+".html";
            break;
          case PDF:
            //options.setOutputFormat("pdf");
            outputFileName = extactReportName(metaData.getReportName())+".pdf";
            break;
        }
        //options.setOutputFileName(iConfig.getOutputDir() + outputFileName);

        // ------------------
/*
        if( options.getOutputFormat().equalsIgnoreCase("html"))
        {
          HTMLRenderOption htmlOptions = new HTMLRenderOption( options);
          htmlOptions.setImageHandler(new HTMLServerImageHandler());
          htmlOptions.setImageDirectory(iConfig.getImageDirectory());
          htmlOptions.setBaseImageURL(metaData.getImageBaseUrl());
          htmlOptions.setHtmlPagination(false);
          htmlOptions.setHtmlRtLFlag(false);
          //Setting this to true removes html and body tags
          htmlOptions.setEmbeddable(false);
          renderTask.setRenderOption(htmlOptions);
        }
        else if( options.getOutputFormat().equalsIgnoreCase("pdf") ){

          PDFRenderOption pdfOptions = new PDFRenderOption( options );
          pdfOptions.setOption( IPDFRenderOption.FIT_TO_PAGE, new Boolean(true) );
          pdfOptions.setOption( IPDFRenderOption.PAGEBREAK_PAGINATION_ONLY, new Boolean(true) );
          renderTask.setRenderOption(pdfOptions);
        }

        // ------------------
        if(renderTask.getRenderOption()==null)
          renderTask.setRenderOption(options);

        renderTask.render();

*/
      }
      catch (Exception e)
      {
        throw new RuntimeException("Failed to renderTask report: "+e.getMessage(), e);
      }
      finally
      {
        //if(renderTask !=null)
        //  renderTask.close();
      }

      return outputFileName;
  }
/*
  private IReportRunnable openCached(String reportName)
      throws EngineException
  {
    IReportRunnable design = cache.get(reportName);
    if(null==design)
    {
      design = engine.openReportDesign( servletContext.getResourceAsStream("/WEB-INF" + iConfig.getReportDir() + "/" + reportName) );
      cache.put(reportName, design);
    }
    return design;
  }
*/
  public IntegrationConfig getIntegrationConfig()
  {
    return iConfig;
  }

  private static String extactReportName(String reportFile)
  {
    return reportFile.substring( 0, reportFile.indexOf("."));
  }

  public State getCurrentState()
  {
    return currentState;
  }

  public List<ReportReference> getReportReferences()
  {
    if( (currentState==State.CREATED || currentState==State.STARTED) == false)
      throw new IllegalStateException("Cannot acccess report references in state " + currentState);

    ArrayList<ReportReference> list = new ArrayList<ReportReference>();
    for(String s : reports.keySet())
      list.add(reports.get(s));
    return list;
  }
}
