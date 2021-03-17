#pragma once
#include <vector>

class DepthmapFilter
{
public:
	unsigned char* SubSample(unsigned char* depthBuf, int bytesPerPixel, int width, int height, int& new_width, int& new_height, int mode = 0, int factor = 3);
	void HoleFill(unsigned char* depthBuf, int bytesPerPixel, int kernel_size, int width, int height, int level, bool horizontal);
	void TemporalFilter(unsigned char* depthBuf, int bytesPerPixel, int width, int height, float alpha, int history);
	void EdgePreServingFilter(unsigned char* depthBuf, int type, int width, int height, int level, float sigma, float lumda);
	void ApplyFilters(unsigned char* depthBuf, unsigned char* subDisparity, int bytesPerPixel, int width, int height, int sub_w, int sub_h, int threshold=64);
	void Reset();
	//static DepthmapFilter* getInstance();
	DepthmapFilter();
	~DepthmapFilter();

private:
	//DepthmapFilter();
	void processTemporalFilter(unsigned char* depthBuf, int bytesPerPixel, int width, int height, float alpha, int history);

private:
	//static DepthmapFilter* instance;
	std::vector<unsigned char*> historyDepthBuffer;
	unsigned char* temproalMap;
	unsigned char* uvMap;
	int deltaZ;
	bool reset;
	int uvMapWidth;
	int uvMapHeight;
};