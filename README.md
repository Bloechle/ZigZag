![ZigZag Logo](zz-logo.png) 

# **ZigZag**

**ZigZag** is a robust algorithm for both image binarization and background removal. Leveraging the strengths of our previous algorithm, YinYang, ZigZag provides superior speed, accuracy, and simplicity.

The **ZigZag.jar** Java archive is generated periodically from the source code for convenience. Be careful, as it may not be the latest version (but should).

**Check out the [demo CodePad](https://d23kqqls.live.codepad.app/)** for a live demonstration of ZigZag in action ([CodePad source](https://codepad.app/edit/d23kqqls)).

## **Requires**

- **Java 8** (that's all)

## **How to Run ZigZag**

### **By calling Java API**

To use the API, create a new instance of **ZigZag** with the desired parameters and call the filter method with the input and output image paths.

```java
ZigZag zigzag = new ZigZag(int size, int percent, int mode);
zigzag.applyFilter(inputImagePath, outputImagePath);
```

**Example:**

```java
ZigZag zigzag = new ZigZag(30, 90, ImageFilter.MODE_BINARY_UPSAMPLED);
zigzag.applyFilter("C:/MyImage.jpg", "C:/MyBinarizedImage.png");
```

### **By launching Java runtime**

To run the tool using the Java runtime, use the following command format (have a watch at 'run-example.cmd') :

```sh
java -cp <zigZagJarPath> zig.zag.ZigZag -size <size> -percent <percent> -mode <mode> -threads <threads> -input <inputFilePath> [-output <outputFilePath>] [-debug <true/false>] [-exit <true/false>]
```

**Example:**

```sh
java -cp "C:/path/ZigZag.jar" zig.zag.ZigZag -size 30 -percent 90 -mode 3 -threads 4 -input "C:/path/MyImage.jpg" -output "C:/path/MyBinarizedImage.png"
```

### **Command Line Options**

- **-input <inputFilePath>**: Path to the input image file or directory (required).
- **-output <outputFilePath>**: Path to the output image file or directory (optional, default is inputFilePath with 'ZZ' postfix).
- **-size <size>**: Windows size, typical values can range from 10 to 100 pixels (optional, default is 30).
- **-percent <percent>**: Mean weight for historical documents, typical values can range from 50 to 100 percent (optional, default is 90).
- **-mode <mode>**: Processing mode (optional, default is 1). Available modes:
  - 0: Standard binarization mode.
  - 1: Upsampled binarization mode (x2).
  - 2: Antialiased binarization mode.
  - 3: Background removal with gray-level foreground.
  - 4: Background removal with color foreground.
- **-threads <threads>**: Number of threads for processing (optional, default is half the number of available processors).
- **-lossless <true/false>**: Enable or disable lossless compression (optional, default is true). 
- **-debug <true/false>**: Enable or disable debug mode (optional, default is false).
- **-exit <true/false>**: Whether to exit the application after processing (optional, default is false).

## **Citation**

If you use this code for your research, please cite the following paper available on ACM Open Access:

```
@inproceedings{10.1145/3685650.3685661,
author = {Bloechle, Jean-Luc and Hennebert, Jean and Gisler, Christophe},
title = {ZigZag: A Robust Adaptive Approach to Non-Uniformly Illuminated Document Image Binarization},
year = {2024},
isbn = {9798400711695},
publisher = {Association for Computing Machinery},
address = {New York, NY, USA},
url = {https://doi.org/10.1145/3685650.3685661},
doi = {10.1145/3685650.3685661},
abstract = {In the era of mobile imaging, the quality of document photos captured by smartphones often suffers due to adverse lighting conditions. Traditional document analysis and optical character recognition systems encounter difficulties with images that have not been effectively binarized, particularly under challenging lighting scenarios. This paper introduces a novel adaptive binarization algorithm optimized for such difficult lighting environments. Unlike many existing methods that rely on complex machine learning models, our approach is streamlined and machine-learning free, designed around integral images to significantly reduce computational and coding complexities. This approach enhances processing speed and improves accuracy without the need for computationally expensive training procedures. Comprehensive testing across various datasets, from smartphone-captured documents to historical manuscripts, validates its effectiveness. Moreover, the introduction of versatile output modes, including color foreground extraction, substantially enhances document quality and readability by effectively eliminating unwanted background artifacts. These enhancements are valuable in mobile document image processing across industries that prioritize efficient and accurate document management, spanning sectors such as banking, insurance, education, and archival management.},
booktitle = {Proceedings of the ACM Symposium on Document Engineering 2024},
articleno = {3},
numpages = {10},
keywords = {OCR, binarization, image processing, image thresholding},
location = {San Jose, CA, USA},
series = {DocEng '24}
}
```

## **License**

**ZigZag** is provided under the terms of **GPLv3**. The GNU General Public License version 3 or GPLv3 is a copyleft license, which means that any derivative works (modifications or extensions) of a GPL-licensed software must also be licensed under the GPL-3 or a compatible license. This requirement ensures that open-source code remains open source.
