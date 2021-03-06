// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License. See License.txt in the project root for license information.

package com.microsoft.commondatamodel.objectmodel.persistence.cdmfolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmCollection;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmCorpusContext;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmDocumentDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmFolderDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmObjectDefinition;
import com.microsoft.commondatamodel.objectmodel.persistence.CdmConstants;
import com.microsoft.commondatamodel.objectmodel.enums.CdmObjectType;
import com.microsoft.commondatamodel.objectmodel.persistence.cdmfolder.types.DataType;
import com.microsoft.commondatamodel.objectmodel.persistence.cdmfolder.types.DocumentContent;
import com.microsoft.commondatamodel.objectmodel.persistence.cdmfolder.types.Import;
import com.microsoft.commondatamodel.objectmodel.utilities.CopyOptions;
import com.microsoft.commondatamodel.objectmodel.utilities.DynamicObjectExtensions;
import com.microsoft.commondatamodel.objectmodel.utilities.JMapper;
import com.microsoft.commondatamodel.objectmodel.utilities.ResolveOptions;
import com.microsoft.commondatamodel.objectmodel.utilities.logger.Logger;

public class DocumentPersistence {
  /**
   * Whether this persistence class has async methods.
   */
  public static final boolean isPersistenceAsync = false;
  
  /**
   * The file format/extension types this persistence class supports.
   */
  public static final String[] formats = { CdmConstants.CDM_EXTENSION };

  public static CdmDocumentDefinition fromObject(final CdmCorpusContext ctx, final String name, final String nameSpace,
                                               final String path, final DocumentContent obj) {
    final CdmDocumentDefinition doc = ctx.getCorpus().makeObject(CdmObjectType.DocumentDef);
    doc.setFolderPath(path);
    doc.setNamespace(nameSpace);

    if (!Strings.isNullOrEmpty(obj.getSchema())) {
      doc.setSchema(obj.getSchema());
    }
    if (DynamicObjectExtensions.hasProperty(obj, "JsonSchemaSemanticVersion") && !Strings
            .isNullOrEmpty(obj.getJsonSchemaSemanticVersion())) {
      doc.setJsonSchemaSemanticVersion(obj.getJsonSchemaSemanticVersion());
    }

    if (DynamicObjectExtensions.hasProperty(obj, "DocumentVersion") && !Strings
            .isNullOrEmpty(obj.getDocumentVersion())) {
      doc.setDocumentVersion(obj.getDocumentVersion());
    }

    if (obj.getImports() != null) {
      for (final Import imp: obj.getImports()) {
        doc.getImports().add(ImportPersistence.fromData(ctx, imp));
      }
    }

    if (obj.getDefinitions() != null) {
      for (int i = 0; i < obj.getDefinitions().size(); i++) {
        final CdmCollection<CdmObjectDefinition> definitions = doc.getDefinitions();
        final JsonNode d = obj.getDefinitions().get(i);
        if (d.has("dataTypeName")) {
          definitions.add(DataTypePersistence.fromData(ctx, JMapper.MAP.convertValue(d, DataType.class)));
        } else if (d.has("purposeName")) {
          definitions.add(PurposePersistence.fromData(ctx, d));
        } else if (d.has("attributeGroupName")) {
          definitions.add(AttributeGroupPersistence.fromData(ctx, d));
        } else if (d.has("traitName")) {
          definitions.add(TraitPersistence.fromData(ctx, d));
        } else if (d.has("entityShape")) {
          definitions.add(ConstantEntityPersistence.fromData(ctx, d));
        } else if (d.has("entityName")) {
          definitions.add(EntityPersistence.fromData(ctx, d));
        }
      }
    }

    return doc;
  }
  
  public static CdmDocumentDefinition fromData(CdmCorpusContext ctx, String docName, String jsonData, CdmFolderDefinition folder) {
    try {
      DocumentContent obj = JMapper.MAP.readValue(jsonData, DocumentContent.class);
      return fromObject(ctx, docName, folder.getNamespace(), folder.getFolderPath(), obj);
    } catch (final Exception e) {
      Logger.error(
          DocumentPersistence.class.getSimpleName(),
          ctx,
          Logger.format("Could not convert '{0}'. Reason '{1}'.", docName, e.getLocalizedMessage()),
          "fromData"
      );
      return null;
    }
  }

  public static DocumentContent toData(final CdmDocumentDefinition instance, final ResolveOptions resOpt,
                                       final CopyOptions options) {
    final DocumentContent documentContent = new DocumentContent();

    documentContent.setSchema(instance.getSchema());
    documentContent.setJsonSchemaSemanticVersion(instance.getJsonSchemaSemanticVersion());
    documentContent.setImports(Utils.listCopyDataAsCdmObject(instance.getImports(), resOpt, options));
    documentContent.setDefinitions(Utils.listCopyDataAsCdmObject(instance.getDefinitions(), resOpt, options));
    documentContent.setDocumentVersion(instance.getDocumentVersion());

    return documentContent;
  }
}
