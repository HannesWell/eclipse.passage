/*******************************************************************************
 * Copyright (c) 2020, 2022 ArSysOp
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     ArSysOp - initial API and implementation
 *     IILS mbH (Hannes Wellmann) - rewrite to use OSHI 6 and to compute properties lazy
 *******************************************************************************/
package org.eclipse.passage.lic.oshi;

import static java.util.Map.entry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.passage.lic.api.inspection.EnvironmentProperty;
import org.eclipse.passage.lic.internal.base.inspection.hardware.BaseBoard;
import org.eclipse.passage.lic.internal.base.inspection.hardware.Computer;
import org.eclipse.passage.lic.internal.base.inspection.hardware.Cpu;
import org.eclipse.passage.lic.internal.base.inspection.hardware.Firmware;
import org.eclipse.passage.lic.internal.base.inspection.hardware.OS;

import oshi.SystemInfo;

final class State {

	private static final Map<EnvironmentProperty, Function<SystemInfo, String>> HARDWARE_PROPERTIES = Map.ofEntries( //
			entry(new OS.Family(), s -> s.getOperatingSystem().getFamily()), //
			entry(new OS.Manufacturer(), s -> s.getOperatingSystem().getManufacturer()), //
			entry(new OS.Version(), s -> s.getOperatingSystem().getVersionInfo().getVersion()), //
			entry(new OS.BuildNumber(), s -> s.getOperatingSystem().getVersionInfo().getBuildNumber()), //

			// Computer env currently uses the wrong family which leads to name clashes:
			// https://github.com/eclipse-passage/passage/issues/1096
			entry(new Computer.Manufacturer(), s -> s.getHardware().getComputerSystem().getManufacturer()), //
			entry(new Computer.Model(), s -> s.getHardware().getComputerSystem().getModel()), //
			entry(new Computer.Serial(), s -> s.getHardware().getComputerSystem().getSerialNumber()), //

			entry(new BaseBoard.Manufacturer(),
					s -> s.getHardware().getComputerSystem().getBaseboard().getManufacturer()), //
			entry(new BaseBoard.Model(), s -> s.getHardware().getComputerSystem().getBaseboard().getModel()), //
			entry(new BaseBoard.Version(), s -> s.getHardware().getComputerSystem().getBaseboard().getVersion()), //
			entry(new BaseBoard.Serial(), s -> s.getHardware().getComputerSystem().getBaseboard().getSerialNumber()), //

			entry(new Firmware.Manufacturer(),
					s -> s.getHardware().getComputerSystem().getFirmware().getManufacturer()), //
			entry(new Firmware.Version(), s -> s.getHardware().getComputerSystem().getFirmware().getVersion()), //
			entry(new Firmware.ReleaseDate(), s -> s.getHardware().getComputerSystem().getFirmware().getReleaseDate()), //
			entry(new Firmware.Name(), s -> s.getHardware().getComputerSystem().getFirmware().getName()), //
			entry(new Firmware.Description(), s -> s.getHardware().getComputerSystem().getFirmware().getDescription()), //

			entry(new Cpu.Vendor(), s -> s.getHardware().getProcessor().getProcessorIdentifier().getVendor()), //
			entry(new Cpu.Family(), s -> s.getHardware().getProcessor().getProcessorIdentifier().getFamily()), //
			entry(new Cpu.Model(), s -> s.getHardware().getProcessor().getProcessorIdentifier().getModel()), //
			entry(new Cpu.Name(), s -> s.getHardware().getProcessor().getProcessorIdentifier().getName()), //
			entry(new Cpu.ProcessorId(), s -> s.getHardware().getProcessor().getProcessorIdentifier().getProcessorID()) //
	);

	private final SystemInfo system = new SystemInfo();
	private final List<Swath<?>> swaths = List.of(new Swath.Disks(system), new Swath.Nets(system));

	boolean hasMatchingValue(EnvironmentProperty property, String expected) {
		String regexp = expected.strip().replace("*", ".*"); //$NON-NLS-1$//$NON-NLS-2$
		if (regexp.equals(".*")) { // shortcut for wildcard-values //$NON-NLS-1$
			return HARDWARE_PROPERTIES.containsKey(property) || swaths.stream().anyMatch(s -> s.hasProperty(property));
		}
		Optional<String> hardwareValue = hardwareValue(property);
		Stream<String> swatValues = swaths.stream().flatMap(s -> s.valuesFor(property));
		return Stream.concat(hardwareValue.stream(), swatValues).anyMatch(v -> v.matches(regexp));
	}

	private Optional<String> hardwareValue(EnvironmentProperty property) {
		Function<SystemInfo, String> getter = HARDWARE_PROPERTIES.get(property);
		return getter != null ? readProperty(getter, system) : Optional.empty();
	}

	static <T> Optional<String> readProperty(Function<T, String> getter, T source) {
		if (getter != null) {
			try {
				return Optional.ofNullable(getter.apply(source));
			} catch (Throwable e) { // native errors, assume absent
			}
		}
		return Optional.empty();
	}

	// compute glance

	String getGlance() {
		StringJoiner out = new StringJoiner("\n"); //$NON-NLS-1$
		HARDWARE_PROPERTIES.entrySet().stream().collect(Collectors.groupingBy(e -> e.getKey().family()))
				.forEach((family, properties) -> {
					out.add(family);
					properties.stream().forEach(p -> {
						Optional<String> value = readProperty(p.getValue(), system);
						appendProperty(p.getKey(), value, out);
					});
				});
		swaths.forEach(swath -> {
			List<Map<EnvironmentProperty, Optional<String>>> allValues = swath.allValues();
			for (int i = 0; i < allValues.size(); i++) {
				out.add(swath.family() + " #" + i); //$NON-NLS-1$
				allValues.get(i).forEach((property, value) -> appendProperty(property, value, out));
			}
		});
		return out.toString();
	}

	private static void appendProperty(EnvironmentProperty property, Optional<String> value, StringJoiner out) {
		value.ifPresent(v -> out.add("\t" + property.name() + ": " + v)); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
