// @flow

type MentionData = {
  channelId: string,
  card: Object
};

export default class AppState {
  lastMentioned: Array<MentionData>;

  constructor() {
    this.lastMentioned = [];
  }

  setLastMentioned: (channelId: string, card: Object) => void;
  setLastMentioned(channelId: string, card: Object) {
    this.lastMentioned = this.lastMentioned.filter(md => md.channelId !== channelId).concat([{
      channelId,
      card
    }]);
  }
}
