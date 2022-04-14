/*******************************************************************************
 * Copyright (c) 2020, 2021 ArSysOp
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     ArSysOp - initial API and implementation
 *******************************************************************************/
package org.eclipse.passage.lic.oshi.tests;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.eclipse.passage.lic.api.EvaluationType;
import org.eclipse.passage.lic.api.LicensingException;
import org.eclipse.passage.lic.api.inspection.EnvironmentProperty;
import org.eclipse.passage.lic.api.inspection.RuntimeEnvironment;
import org.eclipse.passage.lic.api.tests.inspection.RuntimeEnvironmentContractTest;
import org.eclipse.passage.lic.internal.base.inspection.hardware.BaseBoard;
import org.eclipse.passage.lic.internal.base.inspection.hardware.Computer;
import org.eclipse.passage.lic.internal.base.inspection.hardware.Cpu;
import org.eclipse.passage.lic.internal.base.inspection.hardware.Disk;
import org.eclipse.passage.lic.internal.base.inspection.hardware.Firmware;
import org.eclipse.passage.lic.internal.base.inspection.hardware.NetworkInterface;
import org.eclipse.passage.lic.internal.base.inspection.hardware.OS;
import org.eclipse.passage.lic.internal.base.inspection.hardware.OS.Family;
import org.eclipse.passage.lic.oshi.HardwareEnvironment;
import org.junit.Test;

import oshi.SystemInfo;
import oshi.hardware.platform.linux.LinuxHardwareAbstractionLayer;
import oshi.software.os.linux.LinuxOperatingSystem;

@SuppressWarnings("restriction")
public final class HardwareEnvironmentTest extends RuntimeEnvironmentContractTest {

	@Override
	protected RuntimeEnvironment environment() {
		return new HardwareEnvironment();
	}

	@Override
	protected EvaluationType expectedEvaluationType() {
		return new EvaluationType.Hardware();
	}

	@Override
	protected String invalidPropertyValue() {
		return "not-existing-operating-system"; //$NON-NLS-1$
	}

	@Override
	protected Family property() {
		return new OS.Family();
	}

	@Test
	public void testMultipleOSHIReads() throws Exception {

		long start, time = start = System.currentTimeMillis();

		initializeRequiredClasses();

		time = time(time, "class-loading"); //$NON-NLS-1$

		time = measureState(time);
		time = measureState(time);
		time = measureState(time);
		time = measureState(time);
		time = measureState(time);

		time = measureIsAssuptionTrue(new OS.Family(), time);
		time = measureIsAssuptionTrue(new OS.Version(), time);

		time = measureIsAssuptionTrue(new Disk.Serial(), time);
		time = measureIsAssuptionTrue(new Disk.Model(), time);

		time = measureIsAssuptionTrue(new BaseBoard.Serial(), time);
		time = measureIsAssuptionTrue(new BaseBoard.Model(), time);

		time = measureIsAssuptionTrue(new Cpu.ProcessorId(), time);
		time = measureIsAssuptionTrue(new Cpu.Family(), time);

		time = measureIsAssuptionTrue(new Computer.Serial(), time);
		time = measureIsAssuptionTrue(new Computer.Model(), time);

		time = measureIsAssuptionTrue(new Firmware.Manufacturer(), time);
		time = measureIsAssuptionTrue(new Firmware.ReleaseDate(), time);

		time = measureIsAssuptionTrue(new NetworkInterface.HwAddress(), time);
		time = measureIsAssuptionTrue(new NetworkInterface.MacAddress(), time);

		System.out.println("Overall " + (System.currentTimeMillis() - start)); //$NON-NLS-1$
	}

	private void initializeRequiredClasses() throws ClassNotFoundException {
		RuntimeEnvironment environment = new HardwareEnvironment();
		environment.toString();
		init(SystemInfo.class);
		init(LinuxOperatingSystem.class);
		init(LinuxHardwareAbstractionLayer.class);

		List<String> hardwareUnits = Arrays.asList("ComputerSystem", "CentralProcessor", "Baseboard", "Firmware", //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
				"Disks", "Networks"); //$NON-NLS-1$ //$NON-NLS-2$
		for (String unit : hardwareUnits) {
			init(LinuxHardwareAbstractionLayer.class.getClassLoader()
					.loadClass("oshi.hardware.platform.linux.Linux" + unit)); //$NON-NLS-1$
		}
	}

	private void init(Class<?> class1) throws ClassNotFoundException {
		class1.getClassLoader().loadClass(class1.getName());
		try {
			Field field = class1.getDeclaredField("serialVersionUID"); //$NON-NLS-1$
			field.setAccessible(true);
			field.get(null);
		} catch (ReflectiveOperationException e) {
		}
	}

	private long measureIsAssuptionTrue(EnvironmentProperty property, long start) throws LicensingException {
		HardwareEnvironment environment = new HardwareEnvironment();
		environment.isAssuptionTrue(property, ""); //$NON-NLS-1$
		return time(start, property.toString());
	}

	private long measureState(long start) throws LicensingException {
		RuntimeEnvironment environment = new HardwareEnvironment();
		environment.state();
		return time(start, "State"); //$NON-NLS-1$
	}

	private long time(long start, String msg) {
		System.out.println("Reading " + msg + " took " + (System.currentTimeMillis() - start)); //$NON-NLS-1$ //$NON-NLS-2$
		return System.currentTimeMillis();
	}
}
