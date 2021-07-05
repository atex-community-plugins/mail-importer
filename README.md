# mail-importer
Plugin to ingest articles and attachments into content

Usage
=====

Add the following to your project only in the server-integration webapp.

```
<dependency>
  <groupId>com.atex.plugins</groupId>
  <artifactId>mail-importer</artifactId>
  <version>2.6</version>
</dependency>
```

In the top pom.xml you need to add the contentData:

```
<dependency>
  <groupId>com.atex.plugins</groupId>
  <artifactId>mail-importer</artifactId>
  <version>2.6</version>
  <classifier>contentdata</classifier>
  <exclusions>
    <!-- exclude most of the dependencies since we only need the content definitions -->
    <exclusion>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-email</artifactId>
    </exclusion>
    <exclusion>
      <groupId>commons-beanutils</groupId>
      <artifactId>commons-beanutils</artifactId>
    </exclusion>
    <exclusion>
      <groupId>org.springframework</groupId>
      <artifactId>spring-context-support</artifactId>
    </exclusion>
    <exclusion>
      <groupId>org.apache.camel</groupId>
      <artifactId>camel-mail</artifactId>
    </exclusion>
    <exclusion>
      <groupId>org.apache.camel</groupId>
      <artifactId>camel-quartz2</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```

The configure the plugin from the Admin GUI to use the correct E-Mail server etc and enable the plugin.

Extendend configuration can be done in the json field, the following contains all the options that can be specified:

```
{
  /*
    The "defaults" section allow you to specify some defaults that will be applied
    to all the endpoints.
  */
  "defaults": {
    // "webPage" is the externalId of a page that will be used as insert parent id in the InsertionInfoAspectBean
    // "webPage": "la-stampa.2014v1.site",

    // "deskLevel" is the externalId of a page that will be used as security parent.
    // "deskLevel": "dam.assets.incoming.d",

    // "section" will stored in the "section" property
    // "section": "CRONACA",

    // "source" the source property
    // "source": "MAIL",

    // "principalId" is the principalId of the user that will be used to import, defaults to "98".
    // "principalId": "98",
    
    // "minWords" allow you to specify the minimum number of words a body needs to have to import it, this
    // is useful when you want to import just the images contained in the mail without the article.
    // "minWords": 10
    
    // "imageMinSize" allow you to specify the minimum size (in bytes) an image should have to import it.
    // This can be used to skip signature images.
    // "imageMinSize": 60000,
    
    // "dumpFolder", when configured, is a folder where processed email will be written to (in gzip form)
    // so it is easy to gather them for debug purposes.
    // "dumpFolder": "",

    // The "contentTypes" allow you to specify some defaults for specific content types.
    "contentTypes": {
      /*
      "atex.onecms.article": {
        "contentType": "parentDocument",
        "objectType": "article",
        "inputTemplate": "p.DamWireArticle"
      },
      "atex.onecms.image": {
        "contentType": "parentDocument",
        "objectType": "image",
        "inputTemplate": "p.DamWireImage"
      },
      "atex.dam.standard.Graphic": {
        "contentType": "parentDocument",
        "objectType": "graphic",
        "inputTemplate": "p.DamWireGraphic"
      }
      */
    }
  },

  /*
    The "mappings" section allow you to specify a different property name for what it is used
    for each content type.
    
    For articles we set: name, body, headline, lead, section, source.
    For images we set: name, byline, section, width, height, description, section, source.
  */
  "mappings": {
    /*
    "atex.onecms.article": {
      "name": "myname"
    },
    "atex.onecms.image": {
      "contentType": "imageContentType"
    },
    "atex.dam.standard.Graphic": {
    }
    */
  },
  /*
    In the "mailUri" array you can specify additional mailbox to be queryed, if you leave
    the main mailUri field empty you can specify all of them here.
  */
  "mailUri": [
    // for each mailUri the only needed parameter is the "uri" parameter, all the other are
    // optional and will override the same value for the current route.
    /*
    {
      "uri": "imaps://imap.ethereal?username=gerhard.berger",
      "articlePartition": "",
      "imagePartition": "",
      "taxonomyId": "t1",
      "webPage": "",
      "deskLevel": "",
      "section": "",
      "source": "",
      "principalId": "",
      "articleAspect": "",
      "imageAspect": "",
      "minWords": 10,
      "imageMinSize": 60000,
      "dumpFolder": "",
      
      // signatures allow you to skip a part of the body because it contains a signature,
      // since each signature is different you need to configure it based on your emails.
      "signatures": [
      
        // if greater than zero it means these number of line "BEFORE" the identified line
        // will be removed too.
        "before": 3,
        
        // a regular expression to be used to identify a signature, when matching everything
        // after the signature (including the matching line) will not be part of the body.
        "regex": "This communication may contain confidential"
      ]
    },
    {
      "enabled": true,
      "uri": "imaps://imap.ethereal?username=clay.regazzoni",
      "taxonomyId": "t2"
    },
    {
      "enabled": false,
      "uri": "imaps://imap.ethereal?username=ayrton.senna"
    }
    */
  ]
}
```

Image Metadata
--------------

The plugin use the image-metadata-extractor-service to extract exif and iptc tags, you can control the location of the
service by setting the property `-Dimage.metadata.service.url=xxx`, it defaults to `http://localhost:8080/image-metadata-extractor-service/image`.

Integration Server
------------------

To let the integration server know about this plugin you should add:

```
<context:component-scan base-package="com.atex.plugins" />
```

to `applicationContext.xml` in the integration-server webapp.

Customizations
==============

MailParser
----------

You can customize the way the plugin parses an email by implementing the `com.atex.plugins.mailimporter.MailParser`
interface and let the plugins now about it by writing the implementing class name in a text file named 
`src/main/resources/META-INF/services/com.atex.plugins.mailimporter.MailParser`.

MailPublisher
-------------

You can customize the way the plugin publish an email by implementing the `com.atex.plugins.mailimporter.MailPublisher`
interface and let the plugins now about it by writing the implementing class name in a text file named 
`src/main/resources/META-INF/services/com.atex.plugins.mailimporter.MailPublisher`.

