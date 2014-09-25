/*
 * Copyright 2013 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.mortar.screen;

import android.os.Bundle;
import mortar.dagger1support.Dagger1Blueprint;
import com.example.mortar.R;
import com.example.mortar.android.ActionBarOwner;
import com.example.mortar.core.MortarDemoActivityBlueprint;
import com.example.mortar.model.Chat;
import com.example.mortar.model.Chats;
import com.example.mortar.model.Message;
import com.example.mortar.mortarscreen.WithModule;
import com.example.mortar.view.ChatView;
import com.example.mortar.view.Confirmation;
import dagger.Provides;
import flow.Flow;
import flow.HasParent;
import flow.Layout;
import flow.Path;
import javax.inject.Inject;
import javax.inject.Singleton;
import mortar.PopupPresenter;
import mortar.ViewPresenter;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

@Layout(R.layout.chat_view) @WithModule(ChatScreen.Module.class)
public class ChatScreen extends Path implements HasParent {
  private final int conversationIndex;

  public ChatScreen(int conversationIndex) {
    this.conversationIndex = conversationIndex;
  }

  @Override public ChatListScreen getParent() {
    return new ChatListScreen();
  }

  @dagger.Module(injects = ChatView.class, addsTo = MortarDemoActivityBlueprint.Module.class)
  public class Module {
    @Provides Chat provideConversation(Chats chats) {
      return chats.getChat(conversationIndex);
    }
  }

  @Singleton
  public static class Presenter extends ViewPresenter<ChatView> {
    private final Chat chat;
    private final ActionBarOwner actionBar;
    private final PopupPresenter<Confirmation, Boolean> confirmer;

    private Subscription running = Subscriptions.empty();

    @Inject
    public Presenter(Chat chat, ActionBarOwner actionBar) {
      this.chat = chat;
      this.actionBar = actionBar;
      this.confirmer = new PopupPresenter<Confirmation, Boolean>() {
        @Override protected void onPopupResult(Boolean confirmed) {
          if (confirmed) Presenter.this.getView().toast("Haven't implemented that, friend.");
        }
      };
    }

    @Override public void dropView(ChatView view) {
      confirmer.dropView(view.getConfirmerPopup());
      super.dropView(view);
    }

    @Override public void onLoad(Bundle savedInstanceState) {
      if (!hasView()) return;

      ActionBarOwner.Config actionBarConfig = actionBar.getConfig();

      actionBarConfig =
          actionBarConfig.withAction(new ActionBarOwner.MenuAction("End", new Action0() {
            @Override public void call() {
              confirmer.show(
                  new Confirmation("End Chat", "Do you really want to leave this chat?", "Yes",
                      "I guess not"));
            }
          }));

      actionBar.setConfig(actionBarConfig);

      confirmer.takeView(getView().getConfirmerPopup());

      running = chat.getMessages().subscribe(new Action1<Message>() {
        @Override public void call(Message message) {
          if (!hasView()) return;
          getView().getItems().add(message);
        }
      });
    }

    @Override protected void onExitScope() {
      ensureStopped();
    }

    public void onConversationSelected(int position) {
      Flow.get(getView().getContext()).goTo(new MessageScreen(chat.getId(), position));
    }

    public void visibilityChanged(boolean visible) {
      if (!visible) {
        ensureStopped();
      }
    }

    private void ensureStopped() {
      running.unsubscribe();
    }
  }
}
