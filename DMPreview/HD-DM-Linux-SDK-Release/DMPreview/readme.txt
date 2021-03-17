============================================================================================
I.EtronDI Enviroment Setting (Ubuntu x86_64)
============================================================================================
Please run below command:

sudo sh setup_env.sh

============================================================================================
II.Build DMPreview
============================================================================================

(1) Using QtCreator

1-1. Open QtCreator.
1-2. Click "Open Project".
1-3. Choose "./DMPreview/DMPreview.pro".
1-4. Click "Configure Project".
1.5. Build & Run project.

(2) Using QMake

2-1. cd ./DMPreview
2-2. qmake
2-3. make
2-4. Generate binary "DMPreview".
2-5. Refer "./bin/run_DMPreview.sh" to add relative library path.

============================================================================================
