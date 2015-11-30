package gov.usgs.winston.in.metadata;

import edu.iris.Fissures.seed.builder.SeedObjectBuilder;
import edu.iris.Fissures.seed.container.Blockette;
import edu.iris.Fissures.seed.container.SeedObjectContainer;
import edu.iris.Fissures.seed.director.ImportDirector;
import edu.iris.Fissures.seed.director.SeedImportDirector;
import gov.usgs.winston.Instrument;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("deprecation")
public class ImportDataless extends AbstractMetadataImporter {
	
	public static final String me = ImportDataless.class.getName();

	public ImportDataless(String configFile) {
		super(configFile);
	}

	public List<Instrument> readMetadata(String fn)
	{
		LOGGER.fine("Reading " + fn);
		
		List<Instrument> list = new LinkedList<Instrument>();
		try
		{
			DataInputStream ls = new DataInputStream(new BufferedInputStream(
	                new FileInputStream(fn)));

			ImportDirector importDirector = new SeedImportDirector();
			SeedObjectBuilder objectBuilder = new SeedObjectBuilder();
			importDirector.assignBuilder(objectBuilder);  // register the builder with the director
			// begin reading the stream with the construct command
			importDirector.construct(ls);  // construct SEED objects silently
			SeedObjectContainer container = (SeedObjectContainer)importDirector.getBuilder().getContainer();
			
			Object object;
			container.iterate();
			while ((object = container.getNext()) != null)
			{
				Blockette b = (Blockette)object;
				if (b.getType() != 50)
					continue;
				
				Instrument inst = new Instrument();
				inst.setLatitude((Double)b.getFieldVal(4));
				inst.setLongitude((Double)b.getFieldVal(5));
				inst.setHeight((Double)b.getFieldVal(6));
				inst.setName((String)b.getFieldVal(3));
				inst.setDescription((String)b.getFieldVal(9));
				
				LOGGER.fine("found " + inst.toString());
				list.add(inst);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return list;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String configFile;
		String dataless;
		
		if (args.length == 0) {
			System.err.println("Usage: ImportDataless [-c <winston.config>] <dataless>");
			System.exit(1);
		}

		if (args[0].equals("-c")) {
			configFile = args[1];
			dataless = args[2];
		} else {
			configFile = DEFAULT_CONFIG_FILE;
			dataless = args[0];
		}
		
		ImportDataless imp = new ImportDataless(configFile);
		imp.updateInstruments(dataless);
	}
}