package org.eclipse.scanning.test.event;

import static org.eclipse.scanning.api.malcolm.IMalcolmDevice.DATASETS_TABLE_COLUMN_NAME;
import static org.eclipse.scanning.api.malcolm.IMalcolmDevice.DATASETS_TABLE_COLUMN_RANK;
import static org.eclipse.scanning.api.malcolm.IMalcolmDevice.DATASETS_TABLE_COLUMN_TYPE;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dawnsci.analysis.api.persistence.IMarshallerService;
import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.scanning.api.malcolm.MalcolmTable;
import org.eclipse.scanning.api.malcolm.attributes.BooleanArrayAttribute;
import org.eclipse.scanning.api.malcolm.attributes.BooleanAttribute;
import org.eclipse.scanning.api.malcolm.attributes.ChoiceAttribute;
import org.eclipse.scanning.api.malcolm.attributes.NumberArrayAttribute;
import org.eclipse.scanning.api.malcolm.attributes.NumberAttribute;
import org.eclipse.scanning.api.malcolm.attributes.PointGeneratorAttribute;
import org.eclipse.scanning.api.malcolm.attributes.StringArrayAttribute;
import org.eclipse.scanning.api.malcolm.attributes.StringAttribute;
import org.eclipse.scanning.api.malcolm.attributes.TableAttribute;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.example.classregistry.ScanningExampleClassRegistry;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.points.classregistry.ScanningAPIClassRegistry;
import org.eclipse.scanning.points.serialization.PointsModelMarshaller;
import org.eclipse.scanning.test.ScanningTestClassRegistry;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MalcolmAttributesSerializationTest {
	
	private IMarshallerService service;
	
	@Before
	public void create() throws Exception {
		// Non-OSGi for test - do not copy
		
		service = new MarshallerService(
				Arrays.asList(new ScanningAPIClassRegistry(),
						new ScanningExampleClassRegistry(),
						new ScanningTestClassRegistry()),
				Arrays.asList(new PointsModelMarshaller())
				);
	}
	
	@Test
	public void testSerializeMalcolmBooleanAttribute() throws Exception {
		BooleanAttribute attrib = new BooleanAttribute();
		attrib.setName("booleanAttrib");
		attrib.setLabel("Boolean Attribute");
		attrib.setDescription("Description of a boolean attribute");
		attrib.setTags(new String[] { "foo", "bar" });
		attrib.setWriteable(true);
		attrib.setValue(true);
		
		String json = service.marshal(attrib);
		BooleanAttribute newAttrib = service.unmarshal(json, BooleanAttribute.class);
		assertEquals(attrib, newAttrib);
	}
	
	@Test
	public void testSerializeMalcolmBooleanArrayAttribute() throws Exception {
		BooleanArrayAttribute attrib = new BooleanArrayAttribute();
		attrib.setName("booleanArrayAttribute");
		attrib.setLabel("Boolean Array Attribute");
		attrib.setDescription("Description of a boolean array attribute");
		attrib.setTags(new String[] { "foo", "bar" });
		attrib.setWriteable(true);
		attrib.setValue(new boolean[] { true, false, true, true, false });
		
		String json = service.marshal(attrib);
		BooleanArrayAttribute newAttrib = service.unmarshal(json, BooleanArrayAttribute.class);
		assertEquals(attrib, newAttrib);
	}
	
	@Test
	public void testSerializeChoiceAttribute() throws Exception {
		ChoiceAttribute attrib = new ChoiceAttribute();
		attrib.setName("choiceAttribute");
		attrib.setLabel("Choice Attribute");
		attrib.setDescription("Description of a choice attribute");
		attrib.setTags(new String[] { "foo", "bar" });
		attrib.setWriteable(true);
		attrib.setChoices(new String[] { "first", "second", "third" });
		attrib.setValue("second");
		
		String json = service.marshal(attrib);
		ChoiceAttribute newAttrib = service.unmarshal(json, ChoiceAttribute.class);
		assertEquals(attrib, newAttrib);
	}
	
	@Test
	public void testSerializeNumberAttribute() throws Exception {
		NumberAttribute attrib = new NumberAttribute();
		attrib.setName("numberAttribute");
		attrib.setLabel("Number Attribute");
		attrib.setDescription("Description of a number attribute");
		attrib.setTags(new String[] { "foo", "bar" });
		attrib.setWriteable(true);
		attrib.setValue(123.456);
		
		String json = service.marshal(attrib);
		NumberAttribute newAttrib = service.unmarshal(json, NumberAttribute.class);
		assertEquals(attrib, newAttrib);
	}
	
	@Test
	public void testSerializeNumberArrayAttribute() throws Exception {
		NumberArrayAttribute attrib = new NumberArrayAttribute();
		attrib.setName("numberArrayAttribute");
		attrib.setLabel("Number Array Attribute");
		attrib.setDescription("Description of a number array attribute");
		attrib.setTags(new String[] { "foo", "bar" });
		attrib.setWriteable(true);
		attrib.setDtype("double");
		attrib.setValue(new Number[] { 1.2, 3.4, 5.6, 7.8 });
		
		String json = service.marshal(attrib);
		NumberArrayAttribute newAttrib = service.unmarshal(json, NumberArrayAttribute.class);
		assertEquals(attrib, newAttrib);
	}
	
	@Test
	public void testSerializePointGeneratorAttribute() throws Exception {
//		BoundingBox box = new BoundingBox();
//		box.setFastAxisStart(0);
//		box.setSlowAxisStart(0);
//		box.setFastAxisLength(3);
//		box.setSlowAxisLength(3);
//		
//		GridModel gridModel = new GridModel("x", "y");
//		gridModel.setFastAxisPoints(20);
//		gridModel.setSlowAxisPoints(50);
//		gridModel.setBoundingBox(box);
//		
//		CompoundModel<?> compoundModel = new CompoundModel<>(gridModel);
//		
//		IPointGeneratorService genService = new PointGeneratorService();
//		IPointGenerator<?> gen = genService.createCompoundGenerator(compoundModel);
		
		// It seems that point generator attribute just stores a map, so we just test that
		Map<String, String> map = new HashMap<>();
		map.put("key", "value");
		map.put("foo", "bar");
		
		PointGeneratorAttribute attrib = new PointGeneratorAttribute();
		attrib.setName("pointGeneratorAttribute");
		attrib.setLabel("Point Generator Attribute");
		attrib.setDescription("Description of a point generator attribute");
		attrib.setTags(new String[] { "foo", "bar" });
		attrib.setWriteable(true);
		attrib.setValue(map);
		
		String json = service.marshal(attrib);
		PointGeneratorAttribute newAttrib = service.unmarshal(json, PointGeneratorAttribute.class);
		assertEquals(attrib, newAttrib);
	}
	
	@Test
	public void testSerializeStringAttribute() throws Exception {
		StringAttribute attrib = new StringAttribute();
		attrib.setName("stringAttribute");
		attrib.setLabel("String Attribute");
		attrib.setDescription("Description of a string attribute");
		attrib.setTags(new String[] { "foo", "bar" });
		attrib.setWriteable(true);
		attrib.setValue("hello");
		
		String json = service.marshal(attrib);
		StringAttribute newAttrib = service.unmarshal(json, StringAttribute.class);
		assertEquals(attrib, newAttrib);
	}
	
	@Test
	public void testSerializeStringArrayAttribute() throws Exception {
		StringArrayAttribute attrib = new StringArrayAttribute();
		attrib.setName("stringArrayAttribute");
		attrib.setLabel("String Array Attribute");
		attrib.setDescription("Description of a string array attribute");
		attrib.setTags(new String[] { "foo", "bar" });
		attrib.setWriteable(true);
		attrib.setValue(new String[] { "this", "is", "a", "string", "array", "attribute" });
		
		String json = service.marshal(attrib);
		StringArrayAttribute newAttrib = service.unmarshal(json, StringArrayAttribute.class);
		assertEquals(attrib, newAttrib);
	}
	
	@Test
	public void testSerializeTableAttribute() throws Exception {
		Map<String, Class<?>> types = new LinkedHashMap<>();
		// a simplified version of the datasets table 
		types.put(DATASETS_TABLE_COLUMN_NAME, String.class);
		types.put(DATASETS_TABLE_COLUMN_RANK, Integer.class);
		types.put(DATASETS_TABLE_COLUMN_TYPE, String.class);
		
		Map<String, List<Object>> tableData = new HashMap<>();
		tableData.put(DATASETS_TABLE_COLUMN_NAME, Arrays.asList("det.data", "det.sum",
				"x.value", "x.value_set", "y.value", "y.value_set", "I0", "It"));
		tableData.put(DATASETS_TABLE_COLUMN_RANK, Arrays.asList(4, 2, 2, 2, 2, 2, 2, 2));
		tableData.put(DATASETS_TABLE_COLUMN_TYPE, Arrays.asList("primary", "secondary",
				"position_value", "position_set", "position_value", "position_set", "monitor", "monitor"));
		
		MalcolmTable table = new MalcolmTable(tableData, types);
		
		TableAttribute attrib = new TableAttribute();
		attrib.setName("tableAttribute");
		attrib.setLabel("Table Attribute");
		attrib.setDescription("Description of a table attribute");
		attrib.setTags(new String[] { "foo", "bar" });
		attrib.setWriteable(true);
		attrib.setHeadings(new String[] { DATASETS_TABLE_COLUMN_NAME, DATASETS_TABLE_COLUMN_RANK,
				DATASETS_TABLE_COLUMN_TYPE });
		attrib.setValue(table);
		
		String json = service.marshal(attrib);
		TableAttribute newAttrib = service.unmarshal(json, TableAttribute.class);
		assertEquals(attrib, newAttrib);
	}

}
