/**
 * Rapid-prototyping protection schemes with IEC 61850
 *
 * Copyright (c) 2012 Steven Blair
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package rapid61850;

import java.util.Iterator;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import ch.iec._61850._2006.scl.SclPackage;
import ch.iec._61850._2006.scl.TAccessPoint;
import ch.iec._61850._2006.scl.TBDA;
import ch.iec._61850._2006.scl.TDA;
import ch.iec._61850._2006.scl.TDAType;
import ch.iec._61850._2006.scl.TDO;
import ch.iec._61850._2006.scl.TDOType;
import ch.iec._61850._2006.scl.TDataSet;
import ch.iec._61850._2006.scl.TDataTypeTemplates;
import ch.iec._61850._2006.scl.TExtRef;
import ch.iec._61850._2006.scl.TFCDA;
import ch.iec._61850._2006.scl.TIED;
import ch.iec._61850._2006.scl.TSDO;

public abstract class CFunctionCoder extends CFunction {
	
	protected EObject		dataType;	// may be DO, DA, or DataSet
	protected CommsType		commsType;
	protected CoderType		coderType;
	protected String		prefix;
	protected TExtRef		extRef;
	protected String		GSESVInputPath;
	protected SCDAdditionalMappings map;

	public CFunctionCoder(EObject obj, CommsType commsType, CoderType coderType, SCDAdditionalMappings map) {
		super("int");
		
		this.map = map;

		this.commsType = commsType;
		this.coderType = coderType;
		this.dataType = obj;
		
		if (commsType == CommsType.SV && coderType == CoderType.DECODER) {
			this.prefix = "decode_";
		}
		else if (commsType == CommsType.SV && coderType == CoderType.ENCODER) {
			this.prefix = "encode_";
		}
		else if (commsType == CommsType.GSE && coderType == CoderType.DECODER) {
			this.prefix = "ber_decode_";
		}
		else if (commsType == CommsType.GSE && coderType == CoderType.ENCODER) {
			this.prefix = "ber_encode_";
		}
	}
	
	public CFunctionCoder(EObject obj, TExtRef extRef, CommsType commsType, CoderType coderType, SCDAdditionalMappings map) {
		this(obj, commsType, coderType, map);
		this.extRef = extRef;
	}
	
	public String getArgs() {
		if (this.dataType.eClass().getName().equals("TDataSet")) {
			TDataSet dataset = (TDataSet) dataType;
			String datasetName = SCDCodeGenerator.getUniqueDatasetName(dataset);
			
			if (commsType == CommsType.SV && coderType == CoderType.DECODER) {
				return "unsigned char *buf, CTYPE_INT16U smpCnt, struct " + datasetName + " *" + datasetName;
			}
			else if (commsType == CommsType.GSE && coderType == CoderType.DECODER) {
				return "unsigned char *buf, struct " + datasetName + " *" + datasetName;
			}
			else {
				return "unsigned char *buf";
			}
		}
		else {
			return "unsigned char *buf, struct " + getName() + " *" + getName();
		}
	}

	public String getPrefix() {
		return prefix;
	}
	
	public String getName() {
		if (dataType.eClass().getName().equals("TDAType")) {
			return ((TDAType) dataType).getId();
		}
		else if (dataType.eClass().getName().equals("TDOType")) {
			return ((TDOType) dataType).getId();
		}
		else  if (dataType.eClass().getName().equals("TDataSet")) {
			return SCDCodeGenerator.getUniqueDatasetName((TDataSet) dataType);
		}
		
		return null;
	}
	
	public String getFunctionName() {
		return this.prefix + getName();
	}
	
	//public abstract String getItemCoder(EObject obj, Boolean getLength);
	public String getItemCoder(EObject obj, Boolean getLength) {
		EClass objectClass = obj.eClass();
		String itemType = "";
		String variableName = "";
		String enumCast = "";
		String coder = getPrefix();
		Boolean basicDataType = false;
		String source = getName();
		String subItemLinker = "->";
		String assignment = "+=";
		String buffer = "&buf[offset], ";
		String accumulator = "offset";
		
		if (getLength == true) {
			assignment = "=";
			buffer = "";
			accumulator = "len";
		}
		
		if (objectClass == SclPackage.eINSTANCE.getTBDA()) {
			TBDA bda = (TBDA) obj;
			itemType = map.getCoderType(bda);
			
			if (!bda.getBType().toString().equals("Struct")) {
				basicDataType = true;
			}
			if (bda.getBType().toString().equals("Enum")) {
				enumCast = "(CTYPE_ENUM *) ";
			}
			
			variableName = bda.getName().toString();
		}
		else if (objectClass == SclPackage.eINSTANCE.getTDA()) {
			TDA da = (TDA) obj;
			itemType = map.getCoderType(da);
			
			if (!da.getBType().toString().equals("Struct")) {
				basicDataType = true;
			}
			if (da.getBType().toString().equals("Enum")) {
				enumCast = "(CTYPE_ENUM *) ";
			}
			
			variableName = da.getName().toString();
		}
		else if (objectClass == SclPackage.eINSTANCE.getTDO()) {
			TDO dataObject = (TDO) obj;
			
			itemType = dataObject.getType();
			variableName = dataObject.getName();
		}
		else if (objectClass == SclPackage.eINSTANCE.getTSDO()) {
			TSDO sdo = (TSDO) obj;
			
			itemType = sdo.getType();
			variableName = sdo.getName();
		}
		else if (objectClass == SclPackage.eINSTANCE.getTExtRef()) {
			itemType = "extref";
		}
		else if (objectClass == SclPackage.eINSTANCE.getTFCDA()) {
			TFCDA fcda = ((TFCDA) obj);
			
			itemType = map.getCoderType(map.getDataAttribute(fcda));
			variableName = map.getVariableName(fcda);
			
			if (map.getPrintedType(map.getDataAttribute(fcda)) != null) {
				if (!map.getPrintedType(map.getDataAttribute(fcda)).contains("struct")) {
					basicDataType = true;
				}
				if (map.getPrintedType(map.getDataAttribute(fcda)).contains("enum")) {
					enumCast = "(CTYPE_ENUM *) ";
				}
			}
			
			if (coderType == CoderType.ENCODER) {
				String iedName = ((TIED) fcda.eContainer().eContainer().eContainer().eContainer().eContainer().eContainer()).getName();
				String apName = ((TAccessPoint) fcda.eContainer().eContainer().eContainer().eContainer().eContainer()).getName();
				
				source =  iedName + "." + apName + "." + fcda.getLdInst() + "." + map.getLN(fcda).getPrefix() + map.getLN(fcda).getLnType().replaceAll("[^A-Za-z0-9]", "_") + "_" + map.getLN(fcda).getInst();
				
				if (fcda.getDaName() == null || fcda.getDaName().equals("")) {
					variableName = fcda.getDoName();
				}
				else {
					source = source + "." + fcda.getDoName();
					variableName = fcda.getDaName();
				}
				subItemLinker = ".";
			}
		}
		
		if (basicDataType) {
			coder = coder.toUpperCase();
		}
		
		if (commsType == CommsType.SV) {
			return "\toffset += " + coder + itemType + "(&buf[offset], " + enumCast + "&" + source + subItemLinker + variableName + ");\n";
		}
		else {
			return "\t" + accumulator + " " + assignment + " " + coder + itemType + "(" + buffer + enumCast + "&" + source + subItemLinker + variableName + ");\n";
		}
	}
	
	public TDataTypeTemplates getTDataTypeTemplates() {
		String uriFragment = "//@sCL/@dataTypeTemplates";

		return (TDataTypeTemplates) dataType.eResource().getEObject(uriFragment);
	}
	
	//		should cater for:
	//			encoding and decoding (where relevant)
	//			SV and GSE
	//			DO, DA, BDA, SDO, or FCDA (where the FCDA refers to a DO or DA)
	public String getBody() {
		Iterator<EObject> objects = dataType.eContents().iterator();
		String body = "\tint offset = 0;\n\n";
		Boolean gseDecodeNotDataset = false;

		// add tag/length code required for GSE encoding/decoding
		if (commsType == CommsType.GSE) {
			if (dataType.eClass().getName().equals("TDAType") || dataType.eClass().getName().equals("TDOType")) {
				if (coderType == CoderType.ENCODER) {
					body = body.concat("\tbuf[offset++] = 0xA2;\n");
					body = body.concat("\toffset += encodeLength(&buf[offset], ber_get_length_" + getName() + "(" + getName() + "));\n\n");
				}
				else {
					gseDecodeNotDataset = true;
					body = body.concat("\tif (buf[offset++] == 0xA2) {\n");
					body = body.concat("\t\toffset += getLengthFieldSize(buf[offset]);\n\n");
				}
			}
		}
		
		while (objects.hasNext()) {
			EObject obj = objects.next();

			if (gseDecodeNotDataset) {
				body = body.concat("\t");
			}
			body = body.concat(getItemCoder(obj, false));
		}
		
		if (gseDecodeNotDataset) {
			body = body.concat("\t}\n");
		}
		
		body = body.concat("\n\treturn offset;\n");
		
		return body;
	}
	
}
