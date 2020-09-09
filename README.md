# mail-importer
Plugin to ingest articles and attachments into content

Usage
=====

Add the following to your project only in the server-integration webapp.

```
<dependency>
  <groupId>com.atex.plugins</groupId>
  <artifactId>mail-importer</artifactId>
  <version>2.0-SNAPSHOT</version>
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
      "imageAspect": ""
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
