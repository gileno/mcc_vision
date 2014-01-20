package load_capturer;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

public class CPUMeter {
	
	Sigar sigar = new Sigar();
	
	public double getCPUUsagePercent() throws SigarException {
		return CentralMeter.round(sigar.getCpuPerc().getCombined() * 100, 2, 0);
	}
	
	/**
	 * %user: Percentage of CPU utilization that occurred while executing at the user level (application).
	 * @param args
	 * @throws SigarException
	 */
	public double getCPUUsageBytes() throws SigarException {
		return sigar.getCpu().getUser();
	}
}
