package org.sample.controllers;

import org.juzu.Action;
import org.juzu.Render;
import org.juzu.Response;
import org.juzu.Resource;
import org.juzu.application.ApplicationDescriptor;
import org.juzu.template.Template;
import org.juzu.text.Printer;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class Sample
{

   static
   {
      // Generated
      ApplicationDescriptor desc = org.sample.SampleApplication.DESCRIPTOR;
   }

   @Inject
   @Resource("MyTemplate.gtmpl")
   private Template template;

   @Inject
   private Printer printer;

   @Action
   public void action()
   {
      // Render phase
      org.sample.SampleApplication.foo("bar");
   }

   @Render
   public void render() throws IOException
   {
      // A generated template literal for MyTemplate
      org.sample.templates.MyTemplate literal;

      // Render template
      Map<String, Object> data = new HashMap<String, Object>();
      template.render(printer, data);
   }

   @Render
   public void foo(String name) throws IOException
   {
      System.out.println("foo : " + name);
      Map<String, Object> data = new HashMap<String, Object>();
      template.render(printer, data);
   }
}
