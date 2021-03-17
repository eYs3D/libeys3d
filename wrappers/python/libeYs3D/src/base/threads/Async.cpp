// Copyright (C) 2015 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/*
 * Copyright (C) 2015-2017 ICL/ITRI
 * All rights reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of ICL/ITRI and its suppliers, if any.
 * The intellectual and technical concepts contained
 * herein are proprietary to ICL/ITRI and its suppliers and
 * may be covered by Taiwan and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from ICL/ITRI.
 */

#include "base/threads/Async.h"
#include "base/threads/FunctorThread.h"

#include <memory>

namespace libeYs3D    {
namespace base {

namespace {

class SelfDeletingThread final : public FunctorThread {
public:
    using FunctorThread::FunctorThread;

    explicit SelfDeletingThread(const FunctorThread::Functor& func,
                                ThreadFlags flags = ThreadFlags::MaskSignals)
        : FunctorThread(func, flags | ThreadFlags::Detach) {}

    virtual void onExit() override {
        delete this;
    }

    bool wait(intptr_t* exitStatus) override {
        fprintf(stderr, "FATAL: tried to wait on a self deleting thread (for Async)\n");
        abort();
    }
};

}

bool async(const ThreadFunctor& func, ThreadFlags flags) {
    auto thread =
            std::unique_ptr<SelfDeletingThread>(
                new SelfDeletingThread(func, flags));
    if (thread->start()) {
        thread.release();
        return true;
    }

    return false;
}

}  // namespace base
}  // namespace libeYs3D
