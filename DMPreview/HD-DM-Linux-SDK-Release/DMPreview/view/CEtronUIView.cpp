#include "CEtronUIView.h"
#include <QWidget>

CEtronUIView::CEtronUIView()
{

}

void CEtronUIView::UpdateUI()
{
    RUN_ON_UI_THREAD(
    UpdateChildern();
    UpdateSelf();
    );
}

void CEtronUIView::UpdateChildern()
{
    UpdateChildernEtronUIView(dynamic_cast<QWidget *>(this));
}

void CEtronUIView::UpdateChildernEtronUIView(QWidget *pWidget)
{
    if (!pWidget) return;

    QObjectList objcetList = pWidget->children();
    for (QObject *pObject : objcetList){
        CEtronUIView *pUiView = dynamic_cast<CEtronUIView *>(pObject);
        if (!pUiView) {
            UpdateChildernEtronUIView(dynamic_cast<QWidget *>(pObject));
            continue;
        }
        pUiView->UpdateUI();
    }
}

void CEtronUIView::AddUpdateTimer(int mesc)
{
    QWidget *pWidget = dynamic_cast<QWidget *>(this);
    if(!pWidget) return;

    if(m_updateTimer.isActive()){
        m_updateTimer.stop();
    }

    pWidget->connect(&m_updateTimer, SIGNAL(timeout()), SLOT(update()));
    m_updateTimer.start(mesc);
}
