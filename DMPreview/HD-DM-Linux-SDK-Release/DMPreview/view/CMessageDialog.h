#ifndef CMESSAGEDIALOG_H
#define CMESSAGEDIALOG_H
#include <QDialog>
#include <QLabel>
#include "CEtronUIView.h"

class CMessageDialog : public QDialog,
                       public CEtronUIView
{
Q_OBJECT
public:
    CMessageDialog(QString sMessage, QWidget *parent = nullptr);
    ~CMessageDialog();

    QString GetMessage(){ return m_messageLable.text(); }
private:
    QLabel m_messageLable;
};

#endif // CMESSAGEDIALOG_H
