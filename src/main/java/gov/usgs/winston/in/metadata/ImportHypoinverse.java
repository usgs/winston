package gov.usgs.winston.in.metadata;

import gov.usgs.winston.Instrument;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

/**
 * Import station metadata from hypoinverse station file
 * 
 * Format taken from:
 * http://folkworm.ceri.memphis.edu/ew-doc/USER_GUIDE/hypoinv_sta.html
 * 
 * @author tparker
 * 
 */
public class ImportHypoinverse extends AbstractMetadataImporter {

	public static final String me = ImportHypoinverse.class.getName();

	public ImportHypoinverse(String configFile) {
		super(configFile);
	}

	public List<Instrument> readMetadata(String fn) {
		LOGGER.fine("Reading " + fn);

		List<Instrument> list = new LinkedList<Instrument>();
		try {
			String record;
			BufferedReader in = new BufferedReader(new FileReader(fn));
			record = in.readLine();
			
			while (record != null) {
				Instrument inst = new Instrument();
				inst.setName(record.substring(0, 5).trim());

				double lat = Double.parseDouble(record.substring(15, 17));
				lat += Double.parseDouble(record.substring(18, 25).trim()) / 60;
				if (record.substring(25, 26).equals("S"))
					lat *= -1;
				inst.setLatitude(lat);

				double lon = Double.parseDouble(record.substring(26, 29).trim());
				lon += Double.parseDouble(record.substring(30, 36).trim()) / 60;
				if (record.substring(37, 38).equals("W"))
					lon *= -1;
				inst.setLongitude(lon);;

				int height = Integer.parseInt(record.substring(38, 42).trim());
				inst.setHeight(height);

				list.add(inst);
				record = in.readLine();
			}
			in.close();
		} catch (Exception e) {
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

		ImportHypoinverse imp = new ImportHypoinverse(configFile);
		imp.updateInstruments(dataless);
	}

}