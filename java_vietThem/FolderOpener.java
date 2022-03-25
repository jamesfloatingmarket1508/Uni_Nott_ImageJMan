package ij.plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.Prefs;
import ij.VirtualStack;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.plugin.FolderOpener.FolderOpenerDialog;
import ij.process.ImageProcessor;
import ij.util.DicomTools;
import ij.util.StringSorter;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.ColorModel;
import java.io.File;

public class FolderOpener implements PlugIn {
	private static String[] excludedTypes = new String[]{".txt", ".lut", ".roi", ".pty", ".hdr", ".java", ".ijm", ".py",
			".js", ".bsh", ".xml"};
	private static boolean staticSortFileNames = true;
	private static boolean staticOpenAsVirtualStack;
	private boolean convertToRGB;
	private boolean sortFileNames = true;
	private boolean openAsVirtualStack;
	private double scale = 100.0D;
	private int n;
	private int start;
	private int increment;
	private String filter;
	private String legacyRegex;
	private FileInfo fi;
	private String info1;
	private ImagePlus image;
	private boolean saveImage;
	private long t0;

	public static ImagePlus open(String path) {
		FolderOpener fo = new FolderOpener();
		fo.saveImage = true;
		fo.run(path);
		return fo.image;
	}

	public ImagePlus openFolder(String path) {
		this.saveImage = true;
		this.run(path);
		return this.image;
	}

	public void run(String arg) {
		boolean isMacro = Macro.getOptions() != null;
		String directory = null;
		String title;
		if (arg != null && !arg.equals("")) {
			directory = arg;
		} else {
			if (!isMacro) {
				this.sortFileNames = staticSortFileNames;
				this.openAsVirtualStack = staticOpenAsVirtualStack;
			}

			arg = null;
			String title = "Open Image Sequence...";
			title = Macro.getOptions();
			if (title != null) {
				directory = Macro.getValue(title, title, (String) null);
				if (directory != null) {
					directory = OpenDialog.lookupPathVariable(directory);
					File f = new File(directory);
					if (!f.isDirectory() && (f.exists() || directory.lastIndexOf(".") > directory.length() - 5)) {
						directory = f.getParent();
					}
				}

				this.legacyRegex = Macro.getValue(title, "or", "");
				if (this.legacyRegex.equals("")) {
					this.legacyRegex = null;
				}
			}

			if (directory == null) {
				if (Prefs.useFileChooser && !IJ.isMacOSX()) {
					OpenDialog od = new OpenDialog(title, arg);
					directory = od.getDirectory();
					String name = od.getFileName();
					if (name == null) {
						return;
					}
				} else {
					directory = IJ.getDirectory(title);
				}
			}
		}

		if (directory != null) {
			String[] list = (new File(directory)).list();
			if (list != null) {
				title = directory;
				if (directory.endsWith(File.separator) || directory.endsWith("/")) {
					title = directory.substring(0, directory.length() - 1);
				}

				int index = title.lastIndexOf(File.separatorChar);
				if (index != -1) {
					title = title.substring(index + 1);
				}

				if (title.endsWith(":")) {
					title = title.substring(0, title.length() - 1);
				}

				IJ.register(FolderOpener.class);
				list = this.trimFileList(list);
				if (list != null) {
					if (IJ.debugMode) {
						IJ.log("FolderOpener: " + directory + " (" + list.length + " files)");
					}

					int width = 0;
					int height = 0;
					int stackSize = 1;
					int bitDepth = 0;
					ImageStack stack = null;
					double min = Double.MAX_VALUE;
					double max = -1.7976931348623157E308D;
					Calibration cal = null;
					boolean allSameCalibration = true;
					IJ.resetEscape();
					Overlay overlay = null;
					this.n = list.length;
					this.start = 1;
					this.increment = 1;

					try {
						if (isMacro) {
							if (!this.showDialog((ImagePlus) null, list)) {
								return;
							}
						} else {
							for (int i = 0; i < list.length; ++i) {
								Opener opener = new Opener();
								opener.setSilentMode(true);
								IJ.redirectErrorMessages(true);
								ImagePlus imp = opener.openImage(directory, list[i]);
								IJ.redirectErrorMessages(false);
								if (imp != null) {
									width = imp.getWidth();
									height = imp.getHeight();
									bitDepth = imp.getBitDepth();
									if (arg == null && !this.showDialog(imp, list)) {
										return;
									}
									break;
								}
							}

							if (width == 0) {
								IJ.error("Sequence Reader",
										"This folder does not appear to contain\nany TIFF, JPEG, BMP, DICOM, GIF, FITS or PGM files.\n \n   \""
												+ directory + "\"");
								return;
							}
						}

						String pluginName = "Sequence Reader";
						if (this.legacyRegex != null) {
							pluginName = pluginName + "(legacy)";
						}

						list = getFilteredList(list, this.filter, pluginName);
						if (list == null) {
							return;
						}

						IJ.showStatus("");
						this.t0 = System.currentTimeMillis();
						if (this.sortFileNames) {
							list = StringSorter.sortNumerically(list);
						}

						if (this.n < 1) {
							this.n = list.length;
						}

						if (this.start < 1 || this.start > list.length) {
							this.start = 1;
						}

						if (this.start + this.n - 1 > list.length) {
							this.n = list.length - this.start + 1;
						}

						int count = 0;
						int counter = 0;
						ImagePlus imp = null;
						boolean firstMessage = true;
						boolean fileInfoStack = false;

						for (int i = this.start - 1; i < list.length; ++i) {
							if (counter++ % this.increment == 0) {
								Opener opener = new Opener();
								opener.setSilentMode(true);
								IJ.redirectErrorMessages(true);
								if ("RoiSet.zip".equals(list[i])) {
									IJ.open(directory + list[i]);
									imp = null;
								} else if (!this.openAsVirtualStack || stack == null) {
									imp = opener.openImage(directory, list[i]);
									stackSize = imp != null ? imp.getStackSize() : 1;
								}

								IJ.redirectErrorMessages(false);
								if (imp != null && stack == null) {
									width = imp.getWidth();
									height = imp.getHeight();
									bitDepth = imp.getBitDepth();
									this.fi = imp.getOriginalFileInfo();
									ImageProcessor ip = imp.getProcessor();
									min = ip.getMin();
									max = ip.getMax();
									cal = imp.getCalibration();
									if (this.convertToRGB) {
										bitDepth = 24;
									}

									ColorModel cm = imp.getProcessor().getColorModel();
									if (this.openAsVirtualStack) {
										if (stackSize > 1) {
											stack = new FileInfoVirtualStack();
											fileInfoStack = true;
										} else {
											stack = new VirtualStack(width, height, cm, directory);
										}

										((VirtualStack) stack).setBitDepth(bitDepth);
									} else if (this.scale < 100.0D) {
										stack = new ImageStack((int) ((double) width * this.scale / 100.0D),
												(int) ((double) height * this.scale / 100.0D), cm);
									} else {
										stack = new ImageStack(width, height, cm);
									}

									this.info1 = (String) imp.getProperty("Info");
								}

								if (imp != null) {
									if (imp.getWidth() == width && imp.getHeight() == height) {
										String label = imp.getTitle();
										if (stackSize == 1) {
											String info = (String) imp.getProperty("Info");
											if (info != null) {
												label = label + "\n" + info;
											}
										}

										if (imp.getCalibration().pixelWidth != cal.pixelWidth) {
											allSameCalibration = false;
										}

										ImageStack inputStack = imp.getStack();
										Overlay overlay2 = imp.getOverlay();
										int slice;
										if (overlay2 != null && !this.openAsVirtualStack) {
											if (overlay == null) {
												overlay = new Overlay();
											}

											for (slice = 0; slice < overlay2.size(); ++slice) {
												Roi roi = overlay2.get(slice);
												int position = roi.getPosition();
												if (position == 0) {
													roi.setPosition(count + 1);
												}

												overlay.add(roi);
											}
										}

										if (this.openAsVirtualStack) {
											if (fileInfoStack) {
												this.openAsFileInfoStack((FileInfoVirtualStack) stack,
														directory + list[i]);
											} else {
												((VirtualStack) stack).addSlice(list[i]);
											}
										} else {
											for (slice = 1; slice <= stackSize; ++slice) {
												int bitDepth2 = imp.getBitDepth();
												String label2 = label;
												ImageProcessor ip = null;
												if (stackSize > 1) {
													String sliceLabel = inputStack.getSliceLabel(slice);
													if (sliceLabel != null) {
														label2 = sliceLabel;
													} else if (label != null && !label.equals("")) {
														label2 = label + ":" + slice;
													}
												}

												ip = inputStack.getProcessor(slice);
												if (this.convertToRGB) {
													ip = ip.convertToRGB();
													bitDepth2 = 24;
												}

												if (bitDepth2 != bitDepth) {
													if (bitDepth == 8 && bitDepth2 == 24) {
														ip = ip.convertToByte(true);
														bitDepth2 = 8;
													} else if (bitDepth == 32) {
														ip = ip.convertToFloat();
														bitDepth2 = 32;
													} else if (bitDepth == 24) {
														ip = ip.convertToRGB();
														bitDepth2 = 24;
													}
												}

												if (bitDepth2 != bitDepth) {
													IJ.log(list[i] + ": wrong bit depth; " + bitDepth + " expected, "
															+ bitDepth2 + " found");
													break;
												}

												if (this.scale < 100.0D) {
													ip = ip.resize((int) ((double) width * this.scale / 100.0D),
															(int) ((double) height * this.scale / 100.0D));
												}

												if (ip.getMin() < min) {
													min = ip.getMin();
												}

												if (ip.getMax() > max) {
													max = ip.getMax();
												}

												((ImageStack) stack).addSlice(label2, ip);
											}
										}

										++count;
										IJ.showStatus(count + "/" + this.n);
										IJ.showProgress(count, this.n);
										if (count >= this.n) {
											break;
										}

										if (IJ.escapePressed()) {
											IJ.beep();
											break;
										}
									} else {
										IJ.log(list[i] + ": wrong size; " + width + "x" + height + " expected, "
												+ imp.getWidth() + "x" + imp.getHeight() + " found");
									}
								}
							}
						}
					} catch (OutOfMemoryError var35) {
						IJ.outOfMemory("FolderOpener");
						if (stack != null) {
							((ImageStack) stack).trim();
						}
					}

					if (stack != null && ((ImageStack) stack).getSize() > 0) {
						ImagePlus imp2 = new ImagePlus(title, (ImageStack) stack);
						if (imp2.getType() == 1 || imp2.getType() == 2) {
							imp2.getProcessor().setMinAndMax(min, max);
						}

						if (this.fi == null) {
							this.fi = new FileInfo();
						}

						this.fi.fileFormat = 0;
						this.fi.fileName = "";
						this.fi.directory = directory;
						imp2.setFileInfo(this.fi);
						imp2.setOverlay(overlay);
						if (allSameCalibration) {
							if (this.scale != 100.0D && cal.scaled()) {
								cal.pixelWidth /= this.scale / 100.0D;
								cal.pixelHeight /= this.scale / 100.0D;
							}

							if (cal.pixelWidth != 1.0D && cal.pixelDepth == 1.0D) {
								cal.pixelDepth = cal.pixelWidth;
							}

							if (cal.pixelWidth <= 1.0E-4D && cal.getUnit().equals("cm")) {
								cal.pixelWidth *= 10000.0D;
								cal.pixelHeight *= 10000.0D;
								cal.pixelDepth *= 10000.0D;
								cal.setUnit("um");
							}

							imp2.setCalibration(cal);
						}

						if (this.info1 != null && this.info1.lastIndexOf("7FE0,0010") > 0) {
							stack = DicomTools.sort((ImageStack) stack);
							imp2.setStack((ImageStack) stack);
							double voxelDepth = DicomTools.getVoxelDepth((ImageStack) stack);
							if (voxelDepth > 0.0D) {
								if (IJ.debugMode) {
									IJ.log("DICOM voxel depth set to " + voxelDepth + " (" + cal.pixelDepth + ")");
								}

								cal.pixelDepth = voxelDepth;
								imp2.setCalibration(cal);
							}
						}

						if (imp2.getStackSize() == 1) {
							imp2.setProperty("Label", list[0]);
							if (this.info1 != null) {
								imp2.setProperty("Info", this.info1);
							}
						}

						if (arg == null && !this.saveImage) {
							String time = (double) (System.currentTimeMillis() - this.t0) / 1000.0D + " seconds";
							imp2.show(time);
							if (((ImageStack) stack).isVirtual()) {
								overlay = ((ImageStack) stack).getProcessor(1).getOverlay();
								if (overlay != null) {
									imp2.setOverlay(overlay);
								}
							}
						}

						if (this.saveImage) {
							this.image = imp2;
						}
					}

					IJ.showProgress(1.0D);
				}
			}
		}
	}

	private void openAsFileInfoStack(FileInfoVirtualStack stack, String path) {
		FileInfo[] info = Opener.getTiffFileInfo(path);
		if (info != null && info.length != 0) {
			int n = info[0].nImages;
			if (info.length == 1 && n > 1) {
				long size = (long) (this.fi.width * this.fi.height * this.fi.getBytesPerPixel());

				for (int i = 0; i < n; ++i) {
					FileInfo fi = (FileInfo) info[0].clone();
					fi.nImages = 1;
					fi.longOffset = fi.getOffset() + (long) i * (size + (long) fi.gapBetweenImages);
					stack.addImage(fi);
				}
			} else {
				stack.addImage(info[0]);
			}

		}
	}

	boolean showDialog(ImagePlus imp, String[] list) {
		int fileCount = list.length;
		FolderOpenerDialog gd = new FolderOpenerDialog(this, "Sequence Options", imp, list);
		gd.addNumericField("Number of images:", (double) fileCount, 0);
		gd.addNumericField("Starting image:", 1.0D, 0);
		gd.addNumericField("Increment:", 1.0D, 0);
		gd.addNumericField("Scale images:", this.scale, 0, 4, "%");
		gd.addStringField("File name contains:", "", 10);
		gd.setInsets(0, 45, 0);
		gd.addMessage("(enclose regex in parens)", (Font) null, Color.darkGray);
		gd.addCheckbox("Convert_to_RGB", this.convertToRGB);
		gd.addCheckbox("Sort names numerically", this.sortFileNames);
		gd.addCheckbox("Use virtual stack", this.openAsVirtualStack);
		gd.addMessage("10000 x 10000 x 1000 (100.3MB)");
		gd.addHelp("http://imagej.nih.gov/ij/docs/menus/file.html#seq1");
		gd.setSmartRecording(true);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return false;
		} else {
			this.n = (int) gd.getNextNumber();
			this.start = (int) gd.getNextNumber();
			this.increment = (int) gd.getNextNumber();
			if (this.increment < 1) {
				this.increment = 1;
			}

			this.scale = gd.getNextNumber();
			if (this.scale < 5.0D) {
				this.scale = 5.0D;
			}

			if (this.scale > 100.0D) {
				this.scale = 100.0D;
			}

			this.filter = gd.getNextString();
			if (this.legacyRegex != null) {
				this.filter = "(" + this.legacyRegex + ")";
			}

			this.convertToRGB = gd.getNextBoolean();
			this.sortFileNames = gd.getNextBoolean();
			this.openAsVirtualStack = gd.getNextBoolean();
			if (this.openAsVirtualStack) {
				this.scale = 100.0D;
			}

			if (!IJ.macroRunning()) {
				staticSortFileNames = this.sortFileNames;
				staticOpenAsVirtualStack = this.openAsVirtualStack;
			}

			return true;
		}
	}

	public static String[] getFilteredList(String[] list, String filter, String title) {
		boolean isRegex = false;
		if (filter != null && (filter.equals("") || filter.equals("*"))) {
			filter = null;
		}

		if (list != null && filter != null) {
			int i;
			if (title == null) {
				String[] list2 = new String[list.length];

				for (i = 0; i < list.length; ++i) {
					list2[i] = list[i];
				}

				list = list2;
			}

			if (filter.length() >= 2 && filter.startsWith("(") && filter.endsWith(")")) {
				filter = filter.substring(1, filter.length() - 1);
				isRegex = true;
			}

			int filteredImages = 0;

			for (i = 0; i < list.length; ++i) {
				if (isRegex && containsRegex(list[i], filter, title != null && title.contains("(legacy)"))) {
					++filteredImages;
				} else if (list[i].contains(filter)) {
					++filteredImages;
				} else {
					list[i] = null;
				}
			}

			if (filteredImages == 0) {
				if (title != null) {
					if (isRegex) {
						IJ.error(title, "None of the file names contain the regular expression '" + filter + "'.");
					} else {
						IJ.error(title, "None of the " + list.length + " files contain '" + filter + "' in the name.");
					}
				}

				return null;
			} else {
				String[] list2 = new String[filteredImages];
				int j = 0;

				for (int i = 0; i < list.length; ++i) {
					if (list[i] != null) {
						list2[j++] = list[i];
					}
				}

				return list2;
			}
		} else {
			return list;
		}
	}

	private static boolean containsRegex(String name, String regex, boolean legacy) {
		boolean contains = false;

		try {
			if (legacy) {
				contains = name.matches(regex);
			} else {
				contains = name.replaceAll(regex, "").length() != name.length();
			}

			IJ.showStatus("");
		} catch (Exception var7) {
			String msg = var7.getMessage();
			int index = msg.indexOf("\n");
			if (index > 0) {
				msg = msg.substring(0, index);
			}

			IJ.showStatus("Regex error: " + msg);
			contains = true;
		}

		return contains;
	}

	public String[] trimFileList(String[] rawlist) {
		int count = 0;

		for (int i = 0; i < rawlist.length; ++i) {
			String name = rawlist[i];
			if (!name.startsWith(".") && !name.equals("Thumbs.db") && !excludedFileType(name)) {
				++count;
			} else {
				rawlist[i] = null;
			}
		}

		if (count == 0) {
			return null;
		} else {
			String[] list = rawlist;
			if (count < rawlist.length) {
				list = new String[count];
				int index = 0;

				for (int i = 0; i < rawlist.length; ++i) {
					if (rawlist[i] != null) {
						list[index++] = rawlist[i];
					}
				}
			}

			return list;
		}
	}

	public static boolean excludedFileType(String name) {
		if (name == null) {
			return true;
		} else {
			for (int i = 0; i < excludedTypes.length; ++i) {
				if (name.endsWith(excludedTypes[i])) {
					return true;
				}
			}

			return false;
		}
	}

	public void openAsVirtualStack(boolean b) {
		this.openAsVirtualStack = b;
	}

	public void sortFileNames(boolean b) {
		this.sortFileNames = b;
	}

	public String[] sortFileList(String[] list) {
		return StringSorter.sortNumerically(list);
	}
}