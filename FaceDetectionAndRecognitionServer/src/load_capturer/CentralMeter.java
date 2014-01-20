package load_capturer;

import java.io.File;
import java.lang.management.ManagementFactory;

import org.hyperic.sigar.SigarException;

import com.sun.management.OperatingSystemMXBean;

public class CentralMeter {
	private CPUMeter cpuMeter;
	private OperatingSystemMXBean osBean;

	public CentralMeter() {
	
		boolean isLinux = System.getProperty("os.name").toLowerCase().contains("linux")
				|| System.getProperty("os.name").toLowerCase().contains("mac");
		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
		if (isLinux) {
			osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
		} else if (isWindows) {
			cpuMeter = new CPUMeter();
		}
		
	}

//	public static void main(String[] args) throws SigarException, InterruptedException {
//		CentralMeter c = new CentralMeter();
//
//		while (true) {
//			System.out.println(c.getCPUUsage(true));
//			Thread.sleep(1000);
//		}
//	}

	//Between 0 - 100
	public String getCPUUsageOnWindows(boolean percentage) throws SigarException {
		loadOSLibs();
		if (percentage) {
			return Double.toString(cpuMeter.getCPUUsagePercent());
		} else {
			return Double.toString(cpuMeter.getCPUUsageBytes());
		}
	}
	
	//Between 0 - 100
	public String getCPUUsageOnLinux() {
		double systemLoadAverage = osBean.getSystemLoadAverage();
		
		if (systemLoadAverage > 1 ) {
			systemLoadAverage  = 1;
		}
		
		systemLoadAverage = systemLoadAverage * 100;
				
		return String.valueOf(systemLoadAverage);
	}
		
	
	public String getCPUPercentageUsage() {
		boolean isLinux = System.getProperty("os.name").toLowerCase().contains("linux")
				|| System.getProperty("os.name").toLowerCase().contains("mac");
		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
		if (isLinux) {
			return getCPUUsageOnLinux();
		} else if (isWindows) {
			try {
				return getCPUUsageOnWindows(true);
			} catch (SigarException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	public static void loadOSLibs() {
		File executionPath = new File("");
		String entirePath = null;
		String path = executionPath.getAbsolutePath().replace('\\', '/');

		boolean isLinux = System.getProperty("os.name").toLowerCase().contains("linux")
				|| System.getProperty("os.name").toLowerCase().contains("mac");
		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
		if (isLinux) {
			if (System.getProperty("os.arch").toLowerCase().contains("86")) {
				entirePath = path + "/libsigar-x86-linux.so";
			} else if (System.getProperty("os.arch").toLowerCase().contains("64")) {
				entirePath = path + "/libsigar-amd64-linux.so";
			}
			System.load(entirePath);
		} else if (isWindows) {
			if (System.getProperty("os.arch").toLowerCase().contains("86")) {
				entirePath = path + "/sigar-x86-winnt.dll";
			} else if (System.getProperty("os.arch").toLowerCase().contains("64")) {
				entirePath = path + "/sigar-amd64-winnt.dll";
			}
			System.load(entirePath);

		}
	}

	/**
	 * 1 - Valor a arredondar. 2 - Quantidade de casas depois da v�rgula. 3 - Arredondar para cima ou para baixo? Para cima = 0 (ceil) Para baixo = 1 ou qualquer outro inteiro (floor)
	 **/
	public static double round(double valor, int casas, int ceilOrFloor) {
		double arredondado = valor;
		arredondado *= (Math.pow(10, casas));
		if (ceilOrFloor == 0) {
			arredondado = Math.ceil(arredondado);
		} else {
			arredondado = Math.floor(arredondado);
		}
		arredondado /= (Math.pow(10, casas));
		return arredondado;
	}

}
